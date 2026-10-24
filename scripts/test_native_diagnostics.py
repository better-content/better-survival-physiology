import unittest
from audit_native_diagnostics import check_native_texts


class NativeDiagnosticsTest(unittest.TestCase):
    def setUp(self):
        self.complete = 'All 44 required tests passed\n44 GAME TESTS COMPLETE'
        self.application = ('[24Oct2026 22:04:51.077] [modloading-worker-0/DEBUG] [mixin/]: '
                            'Mixing thirst.AddLootTableModifierMixin from better_compat_fixes.mixins.json '
                            'into dev.ghen.thirst.foundation.common.loot.AddLootTableModifier')
        # Verbatim historical native failure, Water gate20261020T224535Z latest.log:58.
        self.real_warn = ('[21Oct2026 09:46:03.457] [main/WARN] [net.minecraftforge.common.loot.LootModifierManager/]: '
                          'Could not decode GlobalLootModifier with json id thirst:add_loot_shipwreck_supply '
                          '- error: Unknown registry key in ResourceKey[minecraft:root / forge:global_loot_modifier_serializers]: thirst:add_loot_table')

    def test_actual_warn_is_failed_operation_in_latest(self):
        with self.assertRaisesRegex(AssertionError, 'failed operations'):
            check_native_texts(self.complete + '\n' + self.real_warn, '', 44)

    def test_actual_warn_is_failed_operation_in_debug(self):
        with self.assertRaisesRegex(AssertionError, 'failed operations'):
            check_native_texts(self.complete, self.real_warn, 44)

    def test_error_fatal_and_mixin_failure_in_either_log(self):
        for cause in ['/ERROR]', '/FATAL]', '[ERROR]', '[FATAL]', 'Mixin apply failed']:
            for in_latest in [True, False]:
                with self.subTest(cause=cause, in_latest=in_latest), self.assertRaises(AssertionError):
                    check_native_texts(self.complete + (cause if in_latest else ''), '' if in_latest else cause, 44)

    def test_completion_count_is_not_a_verdict(self):
        for text in ['', 'All 44 required tests passed', '44 GAME TESTS COMPLETE', self.complete]:
            with self.subTest(text=text), self.assertRaises(AssertionError):
                check_native_texts(text, '', 43)

    def test_reviewed_probe_is_not_blanket_severity_rejection(self):
        check_native_texts(self.complete, self.application + '\n[main/DEBUG] Failed to load resource META-INF/jarjar/metadata.json', 44)
        check_native_texts(self.complete + '\n[main/WARN] Fresh configuration defaults initialized', self.application + '\n[main/DEBUG] native setup', 44)

    def test_application_is_exact_once_and_bound_to_debug(self):
        check_native_texts(self.complete, self.application, 44)
        for debug in ['[main/DEBUG] native setup', self.application + '\n' + self.application,
                      self.application.replace('thirst.AddLootTableModifierMixin', 'thirst.OtherMixin'),
                      self.application.replace('into dev.ghen.thirst', 'into other.thirst'),
                      self.application.replace('better_compat_fixes.mixins.json', 'other.mixins.json'),
                      self.application.replace('/DEBUG]', '/INFO]'),
                      'quoted: ' + self.application]:
            with self.subTest(debug=debug), self.assertRaisesRegex(AssertionError, 'application witness'):
                check_native_texts(self.complete, debug, 44)
        with self.assertRaisesRegex(AssertionError, 'application witness'):
            check_native_texts(self.complete + '\n' + self.application, '[main/DEBUG] native setup', 44)

    def test_missing_actual_debug_evidence_rejected(self):
        with self.assertRaisesRegex(AssertionError, 'DEBUG evidence'):
            check_native_texts(self.complete, '', 44)


if __name__ == '__main__':
    unittest.main()
