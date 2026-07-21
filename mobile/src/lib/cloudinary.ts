const cloudName = process.env.EXPO_PUBLIC_CLOUDINARY_CLOUD_NAME;
const uploadPreset = process.env.EXPO_PUBLIC_CLOUDINARY_UPLOAD_PRESET;

if (!cloudName || !uploadPreset) {
  console.warn('Cloudinary env vars are not set — see .env.example');
}

export async function uploadImageToCloudinary(fileUri: string): Promise<string> {
  const formData = new FormData();
  formData.append('file', {
    uri: fileUri,
    type: 'image/jpeg',
    name: 'upload.jpg',
  } as unknown as Blob);
  formData.append('upload_preset', uploadPreset ?? '');

  const response = await fetch(`https://api.cloudinary.com/v1_1/${cloudName}/image/upload`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    throw new Error(`Cloudinary upload failed: ${response.status}`);
  }

  const data = (await response.json()) as { secure_url: string };
  return data.secure_url;
}
