import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/button';
import { TextField } from '@/components/ui/text-field';
import { ApiError, apiClient } from '@/lib/api-client';
import { setAuthToken } from '@/lib/token-storage';
import { useAuthStore } from '@/store/auth-store';
import type { AuthResponse, LoginRequest, User } from '@/types';

export default function LoginScreen() {
  const setUser = useAuthStore((state) => state.setUser);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailError, setEmailError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [orcidNotice, setOrcidNotice] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  async function login(withEmail: string, withPassword: string) {
    setFormError(null);
    setIsLoading(true);
    try {
      const body: LoginRequest = { email: withEmail, password: withPassword };
      const auth = await apiClient.post<AuthResponse>('/auth/login', body);
      await setAuthToken(auth.access_token);
      const user = await apiClient.get<User>('/users/me');
      setUser(user);
      // Stack.Protected in the root layout swaps to (tabs) automatically once isAuthenticated flips.
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Could not log in. Please try again.');
    } finally {
      setIsLoading(false);
    }
  }

  function handleSubmit() {
    if (!email.trim() || !email.includes('@')) {
      setEmailError('Please enter a valid institution email');
      return;
    }
    setEmailError(null);
    login(email.trim(), password);
  }

  async function handleDevBypassLogin() {
    setIsLoading(true);
    try {
      const devJwt =
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyX2Rldl9hZG1pbiIsImVtYWlsIjoiYWRtaW5AY2l0ZWNpcmNsZS5lZHUiLCJuYW1lIjoiRHIuIEFkbWluIFNjaG9sYXIifQ.dev_signature';
      await setAuthToken(devJwt);
      const mockAdminUser: User = {
        id: 'user_dev_admin',
        name: 'Dr. Admin Scholar',
        avatar_url: '',
        role: 'RESEARCHER',
        institution: 'Cite-Circle AI Lab',
        field_of_study: 'Artificial Intelligence & Machine Learning',
        bio: 'Lead Administrator & Senior Researcher at Cite-Circle.',
        orcid_id: '0000-0002-1825-0097',
        follower_count: 128,
        following_count: 45,
        citation_count: 342,
        external_citation_count: 120,
        publication_count: 15,
        orcid_verified: true,
        is_verified: true,
        interests: ['Machine Learning', 'Computer Vision', 'NLP', 'Neural Networks'],
      };
      setUser(mockAdminUser);
    } catch {
      setFormError('Bypass login failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <View className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <SafeAreaView className="flex-1">
        <ScrollView contentContainerClassName="grow items-center justify-center gap-4 px-6 py-8" keyboardShouldPersistTaps="handled">
          <Text className="font-serif text-4xl font-bold text-academic-navy dark:text-[#5D82AE]">Cite-Circle</Text>
          <Text className="mb-4 text-center text-base text-academic-muted dark:text-[#A6A6AC]">Welcome back, Scholar.</Text>

          <TextField
            label="Email Address"
            placeholder="name@institution.edu"
            autoCapitalize="none"
            keyboardType="email-address"
            value={email}
            onChangeText={(value) => {
              setEmail(value);
              setEmailError(null);
            }}
            errorMessage={emailError}
          />

          <TextField
            label="Password"
            placeholder="••••••••"
            secureTextEntry
            value={password}
            onChangeText={setPassword}
          />

          {formError ? <Text className="text-sm text-red-500">{formError}</Text> : null}

          <Button label="Log In" isLoading={isLoading} onPress={handleSubmit} className="w-full" />

          <Button
            label="⚡ Bypass Login (Dev / Admin Mode)"
            variant="secondary"
            isLoading={isLoading}
            onPress={handleDevBypassLogin}
            className="w-full border-amber-500/50 bg-amber-500/10 text-amber-600 dark:bg-amber-500/20"
          />

          <Pressable onPress={() => router.push('/(auth)/register')} className="py-2">
            <Text className="text-base font-semibold text-academic-maroon dark:text-[#C6635E]">
              Don&apos;t have an account? Sign up
            </Text>
          </Pressable>

          <View className="my-2 h-px w-full bg-academic-gold/20" />

          <Button
            label="Continue with ORCID iD"
            variant="secondary"
            onPress={() => setOrcidNotice(true)}
            className="w-full"
          />
          {orcidNotice ? (
            <Text className="text-center text-sm text-academic-muted dark:text-[#A6A6AC]">
              ORCID sign-in is coming soon — log in with email for now, then link ORCID from your profile.
            </Text>
          ) : null}
        </ScrollView>
      </SafeAreaView>
    </View>
  );
}
