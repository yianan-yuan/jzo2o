export function shouldOpenLocationSetting(authSetting = {}) {
  return authSetting['scope.userLocation'] === false;
}
