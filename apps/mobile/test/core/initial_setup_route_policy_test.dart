import 'package:flutter_test/flutter_test.dart';
import 'package:khazain_al_huda/core/initial_setup_route_policy.dart';

void main() {
  test('new install is redirected to mandatory setup', () {
    expect(
      initialSetupRedirect(
        selectedGovernorateId: null,
        matchedLocation: '/',
        isChangeMode: false,
      ),
      '/setup',
    );
  });

  test('legacy installation with a saved governorate skips first-run setup', () {
    expect(
      initialSetupRedirect(
        selectedGovernorateId: 'babil',
        matchedLocation: '/',
        isChangeMode: false,
      ),
      isNull,
    );
  });

  test('normal setup route cannot be reopened after completion', () {
    expect(
      initialSetupRedirect(
        selectedGovernorateId: 'babil',
        matchedLocation: '/setup',
        isChangeMode: false,
      ),
      '/',
    );
  });

  test('settings can explicitly open change mode', () {
    expect(
      initialSetupRedirect(
        selectedGovernorateId: 'babil',
        matchedLocation: '/setup',
        isChangeMode: true,
      ),
      isNull,
    );
  });

  test('a missing selection cannot bypass setup through a nested route', () {
    expect(
      initialSetupRedirect(
        selectedGovernorateId: '',
        matchedLocation: '/prayer',
        isChangeMode: false,
      ),
      '/setup',
    );
  });
}
