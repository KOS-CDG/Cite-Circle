import * as WebBrowser from 'expo-web-browser';
import { useEffect } from 'react';
import { ActivityIndicator, Modal, View } from 'react-native';

import { useAppTheme } from '@/components/ui/theme-provider';

interface PdfViewerModalProps {
  visible: boolean;
  pdfUrl: string | null;
  onClose: () => void;
}

/**
 * Hands the PDF off to the OS/browser's native renderer via Expo WebBrowser —
 * openBrowserAsync works identically on iOS, Android, and web, unlike an
 * in-app WebView (no reliable cross-platform PDF rendering without an extra
 * native dependency this project doesn't have installed).
 */
export function PdfViewerModal({ visible, pdfUrl, onClose }: PdfViewerModalProps) {
  const { colors } = useAppTheme();
  const isOpening = visible && !!pdfUrl;

  useEffect(() => {
    if (!isOpening || !pdfUrl) return;
    let active = true;
    WebBrowser.openBrowserAsync(pdfUrl).finally(() => {
      if (active) onClose();
    });
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpening, pdfUrl]);

  return (
    <Modal visible={isOpening} transparent animationType="fade" onRequestClose={onClose}>
      <View className="flex-1 items-center justify-center bg-black/40">
        <ActivityIndicator color={colors.accent} />
      </View>
    </Modal>
  );
}
