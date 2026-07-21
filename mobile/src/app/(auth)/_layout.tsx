import { Stack } from 'expo-router';

export default function AuthLayout() {
  return (
    <Stack initialRouteName="splash" screenOptions={{ headerShown: false }}>
      <Stack.Screen name="splash" />
      <Stack.Screen name="onboarding" />
      <Stack.Screen name="login" />
      <Stack.Screen name="register" />
      <Stack.Screen name="profile-setup" />
    </Stack>
  );
}
