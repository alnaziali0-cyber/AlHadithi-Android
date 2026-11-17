import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:khazain_al_huda/core/governorate_repository.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues(<String, Object>{});
  });

  test('uses bundled governorates when the API is unavailable', () async {
    final preferences = await SharedPreferences.getInstance();
    final dio = Dio(BaseOptions(baseUrl: 'http://127.0.0.1:1'));
    final repository = GovernorateRepository(dio, preferences);

    final governorates = await repository.load();

    expect(governorates, hasLength(19));
    expect(governorates.map((item) => item.id).toSet(), hasLength(19));
    expect(governorates.any((item) => item.id == 'babil'), isTrue);
  });
}
