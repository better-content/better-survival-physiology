#!/usr/bin/env python3
"""Narrow owning-gate check: required count and actual latest/DEBUG failed operations."""
from pathlib import Path
import sys
import re

THIRST_APPLICATION = re.compile(
    r'^\[[^\]\r\n]+\] \[[^\]\r\n]+/DEBUG\] \[mixin/\]: '
    r'Mixing thirst\.AddLootTableModifierMixin from better_compat_fixes\.mixins\.json '
    r'into dev\.ghen\.thirst\.foundation\.common\.loot\.AddLootTableModifier$')

FAILURES = ('/ERROR]', '/FATAL]', '[ERROR]', '[FATAL]', 'Mixin apply failed',
            'Could not decode GlobalLootModifier')


def check_native_texts(latest, debug, required):
    if not (f'All {required} required tests passed' in latest
            and f'{required} GAME TESTS COMPLETE' in latest):
        raise AssertionError('Missing exact required native completion')
    failures = [(name, number, line) for name, text in [('latest', latest), ('debug', debug)]
                for number, line in enumerate(text.splitlines(), 1)
                if any(cause in line for cause in FAILURES)]
    if failures:
        raise AssertionError(f'Native failed operations (original logs retained): {failures[:8]}')
    if not debug.strip():
        raise AssertionError('Missing/empty actual native DEBUG evidence')
    applications = sum(bool(THIRST_APPLICATION.fullmatch(line)) for line in debug.splitlines())
    if applications != 1:
        raise AssertionError(f'Expected exactly one native Thirst production Mixin application witness; found {applications}')


if __name__ == '__main__':
    required, latest, debug = sys.argv[1:]
    check_native_texts(Path(latest).read_text(), Path(debug).read_text(), int(required))
