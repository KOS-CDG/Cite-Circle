import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/button';
import { RoleCard } from '@/components/ui/role-card';
import { TextField } from '@/components/ui/text-field';
import { ApiError, apiClient } from '@/lib/api-client';
import { setAuthToken } from '@/lib/token-storage';
import { useAuthStore } from '@/store/auth-store';
import type { AuthResponse, SignupRequest, User, UserRole } from '@/types';

const roles: Array<{ role: UserRole; title: string; description: string; emoji: string }> = [
  { role: 'STUDENT', title: 'Student', description: 'Undergrad / Graduate exploring and discussing research.', emoji: '🎓' },
  { role: 'RESEARCHER', title: 'Researcher', description: 'PhD / Postdoc publishing and pre-reviewing manuscripts.', emoji: '🔬' },
  { role: 'EDUCATOR', title: 'Educator', description: 'Professor / Lecturer organizing circles and teaching classes.', emoji: '📖' },
];

export default function RegisterScreen() {
  const setUser = useAuthStore((state) => state.setUser);

  const [selectedRole, setSelectedRole] = useState<UserRole>('STUDENT');
  const [name, setName] = useState('');
  const [institution, setInstitution] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailError, setEmailError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  async function handleSubmit() {
    if (!email.trim() || !email.includes('@')) {
      setEmailError('Please enter a valid institution email');
      return;
    }
    if (password.length < 8) {
      setFormError('Password must be at least 8 characters');
      return;
    }
    setEmailError(null);
    setFormError(null);
    setIsLoading(true);
    try {
      const body: SignupRequest = {
        email: email.trim(),
        password,
        name: name.trim(),
        institution: institution.trim(),
        field_of_study: '',
      };
      const auth = await apiClient.post<AuthResponse>('/auth/signup', body);
      if (auth.access_token) {
        await setAuthToken(auth.access_token);
        try {
          const user = await apiClient.get<User>('/users/me');
          setUser(user, { needsProfileSetup: true });
        } catch {
          setUser(
            {
              id: auth.user_id || 'user_new',
              name: name.trim() || 'Scholar',
              avatar_url: '',
              role: selectedRole,
              institution: institution.trim(),
              field_of_study: '',
              bio: '',
              orcid_id: '',
              follower_count: 0,
              following_count: 0,
              citation_count: 0,
              external_citation_count: 0,
              publication_count: 0,
              orcid_verified: false,
              is_verified: false,
              interests: [],
            },
            { needsProfileSetup: true }
          );
        }
        router.replace('/(auth)/profile-setup');
      } else {
        setFormError('Account created! Please check your email to confirm your account, then log in.');
      }
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Could not create your account. Please try again.');
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <View className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <SafeAreaView className="flex-1">
        <ScrollView contentContainerClassName="items-center gap-4 px-6 py-8" keyboardShouldPersistTaps="handled">
          <Text className="font-serif text-4xl font-bold text-academic-navy dark:text-[#5D82AE]">Cite-Circle</Text>
          <Text className="mb-2 text-center text-base text-academic-muted dark:text-[#A6A6AC]">
            Join the academic circle.
          </Text>

          <View className="w-full gap-2">
            <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">Choose your scholarly role:</Text>
            {roles.map((option) => (
              <RoleCard
                key={option.role}
                role={option.role}
                title={option.title}
                description={option.description}
                emoji={option.emoji}
                isSelected={selectedRole === option.role}
                onPress={() => setSelectedRole(option.role)}
              />
            ))}
          </View>

          <TextField label="Full Name" placeholder="e.g. Maya Okafor" value={name} onChangeText={setName} />

          <TextField
            label="Institution / Affiliation"
            placeholder="e.g. MIT Media Lab"
            value={institution}
            onChangeText={setInstitution}
          />

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

          <TextField label="Password" placeholder="••••••••" secureTextEntry value={password} onChangeText={setPassword} />

          {formError ? <Text className="text-sm text-red-500">{formError}</Text> : null}

          <Button label="Create Account" isLoading={isLoading} onPress={handleSubmit} className="w-full" />

          <Pressable
            onPress={() => {
              if (router.canGoBack()) {
                router.back();
              } else {
                router.replace('/(auth)/login');
              }
            }}
            className="py-2"
          >
            <Text className="text-base font-semibold text-academic-maroon dark:text-[#C6635E]">
              Already have an account? Log in
            </Text>
          </Pressable>
        </ScrollView>
      </SafeAreaView>
    </View>
  );
}
