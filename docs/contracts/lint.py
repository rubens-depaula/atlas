#!/usr/bin/env python3
"""
ATLAS — linter do pack de contratos v0.2.

Valida:
- JSON Schema draft 2020-12;
- exemplos embutidos;
- messageExamples do Adapter API contra os $defs correspondentes;
- integridade referencial e aderência ao vocabulário;
- coerência semântica de property (semanticType/valueType/dimensão);
- criticidade efetiva de action (incluindo herança do device);
- unicidade de keys dentro de cada device;
- árvore de locations sem ciclos.

    pip install jsonschema
    python3 lint.py

Warnings não falham o build; problems falham.
"""

import json
import sys
from datetime import datetime
from pathlib import Path

from jsonschema import Draft202012Validator
from referencing import Registry, Resource

HERE = Path(__file__).parent

SCHEMAS = {
    "https://schemas.atlas.local/v0.2/device.json": "device-v0.2.json",
    "https://schemas.atlas.local/v0.2/property.json": "property-v0.2.json",
    "https://schemas.atlas.local/v0.2/command.json": "command-v0.2.json",
    "https://schemas.atlas.local/v0.2/event.json": "event-v0.2.json",
    "https://schemas.atlas.local/v0.2/adapter-api.json": "adapter-api-v0.2.json",
    "https://schemas.atlas.local/v0.2/environment.json": "environment-v0.2.json",
}

TERMINAL_STATUSES = {
    "CONFIRMED", "COMPLETED", "REJECTED", "FAILED",
    "TIMEOUT", "CANCELLED", "EXPIRED", "UNKNOWN_OUTCOME",
}

problems: list[str] = []
warnings: list[str] = []


def load(name: str) -> dict:
    return json.loads((HERE / name).read_text(encoding="utf-8"))


def build_registry() -> Registry:
    registry = Registry()
    for uri, filename in SCHEMAS.items():
        registry = registry.with_resource(uri, Resource.from_contents(load(filename)))
    return registry


def duplicates(values):
    seen, dup = set(), set()
    for value in values:
        if value in seen:
            dup.add(value)
        seen.add(value)
    return sorted(dup)


def parse_timestamp(value: str):
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except Exception:
        return None


def check_schemas_and_examples(registry: Registry) -> None:
    for uri, filename in SCHEMAS.items():
        schema = load(filename)
        try:
            Draft202012Validator.check_schema(schema)
        except Exception as exc:
            problems.append(f"{filename}: schema inválido — {exc}")
            continue

        validator = Draft202012Validator(schema, registry=registry)
        for i, example in enumerate(schema.get("examples", [])):
            for err in sorted(validator.iter_errors(example), key=lambda e: list(e.path)):
                problems.append(f"{filename} exemplo[{i}] {list(err.path)}: {err.message}")


def validate_adapter_message_examples(registry: Registry) -> None:
    uri = "https://schemas.atlas.local/v0.2/adapter-api.json"
    doc = load("adapter-api-v0.2.json")
    examples = doc.get("messageExamples", {})
    mapping = {
        "inboundObservation": "InboundMessage",
        "inboundAvailability": "InboundMessage",
        "inboundCommandUpdate": "InboundMessage",
        "inboundDeviceAppeared": "InboundMessage",
        "inboundDeviceDisappeared": "InboundMessage",
        "dispatchRequest": "DispatchRequest",
        "dispatchAccepted": "DispatchResult",
        "dispatchRejected": "DispatchResult",
        "discovery": "DiscoveredDevice",
    }

    for key, def_name in mapping.items():
        if key not in examples:
            problems.append(f"adapter-api messageExamples: faltando {key!r}")
            continue
        validator = Draft202012Validator(
            {"$ref": f"{uri}#/$defs/{def_name}"},
            registry=registry,
        )
        for err in validator.iter_errors(examples[key]):
            problems.append(
                f"adapter-api messageExamples[{key!r}] contra {def_name} {list(err.path)}: {err.message}"
            )


def check_property_semantics(prop, tag, semantic_by_key, unit_by_key):
    semantic = semantic_by_key.get(prop.get("semanticType"))
    if not semantic:
        problems.append(f"{tag}: semanticType {prop.get('semanticType')!r} fora do vocabulário")
        return

    unit = unit_by_key.get(prop.get("unit"))
    if not unit:
        problems.append(f"{tag}: unit {prop.get('unit')!r} fora do vocabulário")
        return

    if prop.get("valueType") != semantic.get("valueType"):
        problems.append(
            f"{tag}: valueType {prop.get('valueType')!r} incompatível com "
            f"semanticType {prop.get('semanticType')!r} ({semantic.get('valueType')!r})"
        )

    canonical = unit_by_key.get(semantic.get("canonicalUnit"))
    if canonical and unit.get("dimension") != canonical.get("dimension"):
        problems.append(
            f"{tag}: unit {prop.get('unit')!r} ({unit.get('dimension')}) incompatível com "
            f"semanticType {prop.get('semanticType')!r} ({canonical.get('dimension')})"
        )


def check_discovered_device(dev, tag, semantic_by_key, unit_by_key, device_classes, system_events):
    if dev.get("deviceClass") not in device_classes:
        problems.append(f"{tag}: deviceClass {dev.get('deviceClass')!r} fora do vocabulário")

    props = dev.get("properties", [])
    actions = dev.get("actions", [])
    events = dev.get("events", [])

    for family, items in (("property", props), ("action", actions), ("event", events)):
        keys = [x.get("key") for x in items if isinstance(x, dict)]
        for dup in duplicates(keys):
            problems.append(f"{tag}: {family} key duplicada {dup!r}")

    for prop in props:
        check_property_semantics(prop, f"{tag} property {prop.get('key')!r}", semantic_by_key, unit_by_key)

    for event in events:
        if event.get("key") in system_events:
            problems.append(f"{tag}: evento semântico {event.get('key')!r} colide com evento SYSTEM")


def check_cross_consistency() -> None:
    vocab = load("vocabulary-v0.2.json")
    unit_by_key = {u["key"]: u for u in vocab["units"]}
    semantic_by_key = {s["key"]: s for s in vocab["semanticTypes"]}
    device_classes = set(vocab["deviceClasses"])
    system_events = {
        t for group in vocab["systemEventTypes"].values()
        if isinstance(group, list) for t in group
    }

    # --- devices -----------------------------------------------------------
    for i, dev in enumerate(load("device-v0.2.json").get("examples", [])):
        tag = f"device[{i}] {dev.get('name')!r}"
        actions = dev["actions"]
        props = dev["properties"]
        events = dev["events"]
        action_keys = {a["key"] for a in actions}
        property_keys = {p["key"] for p in props}

        if dev["deviceClass"] not in device_classes:
            problems.append(f"{tag}: deviceClass {dev['deviceClass']!r} fora do vocabulário")

        for family, items in (("property", props), ("action", actions), ("event", events)):
            for dup in duplicates([item["key"] for item in items]):
                problems.append(f"{tag}: {family} key duplicada {dup!r}")

        for prop in props:
            check_property_semantics(prop, f"{tag} property {prop['key']!r}", semantic_by_key, unit_by_key)
            if prop["deviceId"] != dev["id"]:
                problems.append(f"{tag}: property {prop['key']!r} com deviceId divergente")
            if not prop["readOnly"] and prop.get("writeAction") not in action_keys:
                problems.append(f"{tag}: writeAction {prop.get('writeAction')!r} não existe em actions")

        for action in actions:
            effective_criticality = action.get("criticality", dev.get("defaultCriticality"))
            for dup in duplicates([p["key"] for p in action["parameters"]]):
                problems.append(f"{tag}: action {action['key']!r} tem parâmetro duplicado {dup!r}")
            for param in action["parameters"]:
                if param["unit"] not in unit_by_key:
                    problems.append(f"{tag}: action {action['key']!r} usa unit {param['unit']!r} fora do vocabulário")
            if action.get("cancelAction") and action["cancelAction"] not in action_keys:
                problems.append(f"{tag}: cancelAction {action['cancelAction']!r} não existe")
            if action.get("durable") and effective_criticality == "CRITICAL" and not action.get("cancelAction"):
                problems.append(
                    f"{tag}: action {action['key']!r} é durável e efetivamente CRITICAL "
                    f"(incluindo herança) sem cancelAction"
                )
            if action.get("durationParameter"):
                if action["durationParameter"] not in {p["key"] for p in action["parameters"]}:
                    problems.append(f"{tag}: durationParameter {action['durationParameter']!r} não existe")
            for affected in action.get("affectsProperties", []):
                if affected not in property_keys:
                    problems.append(f"{tag}: affectsProperties {affected!r} não existe")

        for event in events:
            if event["key"] in system_events:
                problems.append(f"{tag}: evento semântico {event['key']!r} colide com evento SYSTEM")
            for dup in duplicates([p["key"] for p in event.get("payloadFields", [])]):
                problems.append(f"{tag}: evento {event['key']!r} tem payloadField duplicado {dup!r}")

    # --- standalone property examples ------------------------------------
    for i, doc in enumerate(load("property-v0.2.json").get("examples", [])):
        desc, state = doc["descriptor"], doc["state"]
        tag = f"property[{i}] {desc.get('key')!r}"
        check_property_semantics(desc, tag, semantic_by_key, unit_by_key)
        if (desc["deviceId"], desc["key"]) != (state["deviceId"], state["key"]):
            problems.append(f"{tag}: descriptor e state apontam endereços diferentes")
        if "unit" in state:
            problems.append(f"{tag}: state não deve carregar unit — a autoridade é o descriptor")
        if state.get("confidence") == "ASSUMED" and not state.get("assumedFromCommandId"):
            problems.append(f"{tag}: confidence=ASSUMED sem assumedFromCommandId")

    # --- events ------------------------------------------------------------
    for i, ev in enumerate(load("event-v0.2.json").get("examples", [])):
        tag = f"event[{i}] {ev.get('type')!r}"
        if ev["category"] == "SYSTEM" and ev["type"] not in system_events:
            problems.append(f"{tag}: evento SYSTEM fora do vocabulário fechado")
        if ev["category"] == "SEMANTIC" and ev["type"] in system_events:
            problems.append(f"{tag}: evento SEMANTIC colide com o vocabulário de sistema")
        unit = ev.get("payload", {}).get("unit")
        if unit and unit not in unit_by_key:
            problems.append(f"{tag}: unit {unit!r} fora do vocabulário")

        occurred = parse_timestamp(ev["occurredAt"])
        recorded = parse_timestamp(ev["recordedAt"])
        if occurred and recorded and recorded < occurred:
            warnings.append(
                f"{tag}: recordedAt anterior a occurredAt — possível clock skew do dispositivo; "
                f"não tratado como erro estrutural"
            )

    # --- commands ----------------------------------------------------------
    for i, cmd in enumerate(load("command-v0.2.json").get("examples", [])):
        tag = f"command[{i}] {cmd['target']['action']!r}"
        if cmd["status"] != cmd["statusHistory"][-1]["status"]:
            problems.append(f"{tag}: status divergente do último statusHistory")
        if cmd["actor"]["type"] == "AUTOMATION" and not cmd["causality"].get("cause"):
            problems.append(f"{tag}: comando de automação sem causality.cause")
        terminal = cmd["status"] in TERMINAL_STATUSES
        if terminal and cmd.get("result") is None:
            problems.append(f"{tag}: estado terminal sem result")
        if not terminal and cmd.get("result") is not None:
            problems.append(f"{tag}: estado não-terminal com result preenchido")
        if terminal and not cmd.get("completedAt"):
            problems.append(f"{tag}: estado terminal sem completedAt")
        if cmd.get("executingSince") and not cmd.get("expectedCompletionAt"):
            problems.append(f"{tag}: EXECUTING sem expectedCompletionAt — overrun não seria detectável")

    # --- adapter messageExamples contra o vocabulário --------------------
    adapter_examples = load("adapter-api-v0.2.json").get("messageExamples", {})
    discovery = adapter_examples.get("discovery")
    if discovery:
        check_discovered_device(
            discovery, "adapter discovery", semantic_by_key, unit_by_key, device_classes, system_events
        )
    appeared = adapter_examples.get("inboundDeviceAppeared", {}).get("deviceAppeared")
    if appeared:
        check_discovered_device(
            appeared, "adapter inboundDeviceAppeared", semantic_by_key, unit_by_key, device_classes, system_events
        )
    for key, example in adapter_examples.items():
        if key.startswith("inbound") and example.get("kind") == "OBSERVATION":
            unit = example.get("observation", {}).get("unit")
            if unit not in unit_by_key:
                problems.append(f"adapter {key}: unit {unit!r} fora do vocabulário")

    # --- environment / locations ------------------------------------------
    for i, blk in enumerate(load("environment-v0.2.json").get("examples", [])):
        env, locations = blk["environment"], blk["locations"]
        ids = {loc["id"] for loc in locations}
        for loc in locations:
            if loc["environmentId"] != env["id"]:
                problems.append(f"env[{i}]: location {loc['name']!r} em outro environment")
            if loc["parentId"] and loc["parentId"] not in ids:
                problems.append(f"env[{i}]: location {loc['name']!r} com parentId órfão")
        roots = [loc for loc in locations if loc["parentId"] is None]
        if len(roots) != 1:
            problems.append(f"env[{i}]: esperava 1 raiz, encontrou {len(roots)}")

        # Ciclos são inválidos. Profundidade não tem limite semântico fixo.
        by_id = {loc["id"]: loc for loc in locations}
        for loc in locations:
            seen, cur = set(), loc
            while cur["parentId"]:
                if cur["id"] in seen:
                    problems.append(f"env[{i}]: ciclo na árvore em {loc['name']!r}")
                    break
                seen.add(cur["id"])
                parent = by_id.get(cur["parentId"])
                if parent is None:
                    break  # parent órfão já foi reportado acima
                cur = parent


def main() -> int:
    registry = build_registry()
    check_schemas_and_examples(registry)
    if not problems:
        validate_adapter_message_examples(registry)
    if not problems:
        check_cross_consistency()

    if warnings:
        print(f"⚠ {len(warnings)} warning(s):\n")
        for w in warnings:
            print(f"  {w}")
        print()

    if problems:
        print(f"✗ {len(problems)} problema(s):\n")
        for p in problems:
            print(f"  {p}")
        return 1

    print("✓ schemas válidos, exemplos válidos, messageExamples válidos e consistência cruzada ok")
    return 0


if __name__ == "__main__":
    sys.exit(main())
