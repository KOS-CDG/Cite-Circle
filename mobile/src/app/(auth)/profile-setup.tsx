import * as ImagePicker from 'expo-image-picker';
import { Camera, User as UserIcon } from 'lucide-react-native';
import { useState } from 'react';
import { Image, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/button';
import { Chip } from '@/components/ui/chip';
import { useAppTheme } from '@/components/ui/theme-provider';
import { TextField } from '@/components/ui/text-field';
import { ApiError, apiClient } from '@/lib/api-client';
import { uploadImageToCloudinary } from '@/lib/cloudinary';
import { useAuthStore } from '@/store/auth-store';
import type { User, UserUpdate } from '@/types';

const suggestedInterests = [
  'HCI', 'Machine Learning', 'NLP', 'Computer Science',
  'Genomics', 'Epidemiology', 'Bioinformatics', 'Neuroscience',
  'Climate Change', 'Renewable Energy', 'Materials Science', 'Physics',
  'Cognitive Psychology', 'Economics', 'Digital Humanities', 'History',
];

const MIN_INTERESTS = 3;

export default function ProfileSetupScreen() {
  const { colors } = useAppTheme();
  const currentUser = useAuthStore((state) => state.user);
  const completeProfileSetup = useAuthStore((state) => state.completeProfileSetup);

  const [step, setStep] = useState<1 | 2>(1);
  const [name, setName] = useState(currentUser?.name ?? '');
  const [institution, setInstitution] = useState(currentUser?.institution ?? '');
  const [bio, setBio] = useState(currentUser?.bio ?? '');
  const [avatarUrl, setAvatarUrl] = useState(currentUser?.avatar_url ?? '');
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);
  const [selectedInterests, setSelectedInterests] = useState<Set<string>>(new Set());
  const [isSaving, setIsSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  async function pickAvatar() {
    const permission = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permission.granted) {
      setFormError('Photo library access is needed to set an avatar.');
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      quality: 0.8,
      allowsEditing: true,
      aspect: [1, 1],
    });
    if (result.canceled || !result.assets[0]) return;

    setFormError(null);
    setIsUploadingAvatar(true);
    try {
      const uploadedUrl = await uploadImageToCloudinary(result.assets[0].uri);
      setAvatarUrl(uploadedUrl);
    } catch {
      setFormError('Avatar upload failed. Please try again.');
    } finally {
      setIsUploadingAvatar(false);
    }
  }

  function toggleInterest(interest: string) {
    setSelectedInterests((current) => {
      const next = new Set(current);
      if (next.has(interest)) {
        next.delete(interest);
      } else {
        next.add(interest);
      }
      return next;
    });
  }

  async function finishSetup() {
    setFormError(null);
    setIsSaving(true);
    try {
      const body: UserUpdate = {
        name: name.trim(),
        institution: institution.trim(),
        bio: bio.trim(),
        avatar_url: avatarUrl,
        interests: Array.from(selectedInterests),
      };
      const updated = await apiClient.put<User>('/users/me', body);
      completeProfileSetup(updated);
      // Stack.Protected in the root layout swaps to (tabs) automatically once needsProfileSetup clears.
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Could not save your profile. Please try again.');
    } finally {
      setIsSaving(false);
    }
  }

  const step1Valid = name.trim().length > 0 && institution.trim().length > 0;
  const step2Valid = selectedInterests.size >= MIN_INTERESTS;

  return (
    <View className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <SafeAreaView className="flex-1">
        <View className="h-1.5 w-full bg-academic-gold/15">
          <View className="h-full bg-academic-gold" style={{ width: `${(step / 2) * 100}%` }} />
        </View>

        <View className="flex-row items-center justify-between px-6 py-4">
          <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">Setup Profile</Text>
          <Text className="text-sm text-academic-muted dark:text-[#A6A6AC]">Step {step} of 2</Text>
        </View>

        <ScrollView contentContainerClassName="gap-4 px-6 pb-4" keyboardShouldPersistTaps="handled">
          {step === 1 ? (
            <View className="items-center gap-4 py-2">
              <Pressable
                onPress={pickAvatar}
                disabled={isUploadingAvatar}
                className="h-24 w-24 items-center justify-center overflow-hidden rounded-full border-2 border-academic-maroon bg-academic-parchment dark:border-[#C6635E] dark:bg-[#1F1F26]"
              >
                {avatarUrl ? (
                  <Image source={{ uri: avatarUrl }} className="h-full w-full" />
                ) : (
                  <UserIcon size={40} color={colors.textSecondary} />
                )}
                <View className="absolute bottom-0 right-0 h-7 w-7 items-center justify-center rounded-full border-2 border-white bg-academic-maroon dark:border-[#1F1F26] dark:bg-[#C6635E]">
                  <Camera size={14} color="#FBF9F4" />
                </View>
              </Pressable>

              <TextField label="Full Name" placeholder="e.g. Maya Okafor" value={name} onChangeText={setName} />
              <TextField
                label="Institution / Affiliation"
                placeholder="e.g. MIT Media Lab"
                value={institution}
                onChangeText={setInstitution}
              />
              <TextField
                label="Bio"
                placeholder="A short line about your research interests"
                value={bio}
                onChangeText={setBio}
                multiline
                numberOfLines={3}
              />
            </View>
          ) : (
            <View className="gap-3 py-2">
              <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">
                Select your research fields:
              </Text>
              <Text className="text-sm text-academic-muted dark:text-[#A6A6AC]">
                Select at least {MIN_INTERESTS} interests to personalize your feed.
              </Text>
              <View className="flex-row flex-wrap gap-2">
                {suggestedInterests.map((interest) => (
                  <Chip
                    key={interest}
                    label={interest}
                    selected={selectedInterests.has(interest)}
                    onPress={() => toggleInterest(interest)}
                  />
                ))}
              </View>
            </View>
          )}

          {formError ? <Text className="text-sm text-red-500">{formError}</Text> : null}
        </ScrollView>

        <View className="flex-row gap-3 border-t border-academic-gold/15 px-6 py-4">
          {step === 2 ? (
            <Button label="Back" variant="secondary" onPress={() => setStep(1)} className="flex-1" />
          ) : null}
          <Button
            label={step === 2 ? 'Finish' : 'Continue'}
            isLoading={isSaving}
            disabled={step === 1 ? !step1Valid : !step2Valid}
            onPress={() => (step === 1 ? setStep(2) : finishSetup())}
            className="flex-1"
          />
        </View>
      </SafeAreaView>
    </View>
  );
}
