import { Stack, useLocalSearchParams } from 'expo-router';
import { Bookmark, BookmarkCheck, Check, ChevronDown, FileText, Folder, Plus, Quote, Sparkles, TriangleAlert, X } from 'lucide-react-native';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Modal, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { PdfViewerModal } from '@/components/pdf-viewer-modal';
import { Button } from '@/components/ui/button';
import { ScholarlyText } from '@/components/ui/scholarly-text';
import { useAppTheme } from '@/components/ui/theme-provider';
import { ApiError, apiClient } from '@/lib/api-client';
import type { AiReviewReport, AiSuggestionSeverity, Citation, Paper, ReviewRequest, Shelf, ShelfCreate } from '@/types';

const DEFAULT_SHELF_NAME = 'Saved';

const VERDICT_COLORS: Record<string, string> = {
  ACCEPT: '#2E7D4F',
  MINOR_REVISIONS: '#8A6D1D',
  MAJOR_REVISIONS: '#B5651D',
  REJECT: '#B3261E',
};

const SEVERITY_LABELS: Record<AiSuggestionSeverity, string> = {
  MINOR: 'Minor',
  MODERATE: 'Moderate',
  NEEDS_ATTENTION: 'Needs attention',
};

const SEVERITY_COLORS: Record<AiSuggestionSeverity, string> = {
  MINOR: '#2E7D4F',
  MODERATE: '#8A6D1D',
  NEEDS_ATTENTION: '#B3261E',
};

function formatCitation(paper: Paper): string {
  const parts = [paper.title.trim()];
  if (paper.journal) parts.push(paper.journal);
  parts.push(String(paper.year));
  let citation = parts.filter(Boolean).join('. ') + '.';
  if (paper.doi) citation += ` DOI: ${paper.doi}`;
  return citation;
}

export default function PaperDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { colors } = useAppTheme();

  const [paper, setPaper] = useState<Paper | null>(null);
  const [shelves, setShelves] = useState<Shelf[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [isSaving, setIsSaving] = useState(false);
  const [isCiting, setIsCiting] = useState(false);
  const [showShelfPicker, setShowShelfPicker] = useState(false);
  const [showPdf, setShowPdf] = useState(false);

  const [review, setReview] = useState<AiReviewReport | null>(null);
  const [reviewState, setReviewState] = useState<'idle' | 'loading' | 'error'>('idle');
  const [reviewError, setReviewError] = useState<string | null>(null);
  const [isReviewExpanded, setIsReviewExpanded] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [paperData, shelvesData] = await Promise.all([
        apiClient.get<Paper>(`/papers/${id}`),
        apiClient.get<Shelf[]>('/shelves'),
      ]);
      setPaper(paperData);
      setShelves(shelvesData);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not load this paper.');
    }
  }, [id]);

  useEffect(() => {
    setIsLoading(true);
    load().finally(() => setIsLoading(false));
  }, [load]);

  const isSaved = paper ? shelves.some((shelf) => shelf.paper_ids.includes(paper.id)) : false;

  async function ensureDefaultShelf(): Promise<Shelf> {
    const existing = shelves.find((shelf) => shelf.name === DEFAULT_SHELF_NAME);
    if (existing) return existing;
    const body: ShelfCreate = { name: DEFAULT_SHELF_NAME, description: 'Papers you saved' };
    const created = await apiClient.post<Shelf>('/shelves', body);
    setShelves((current) => [...current, created]);
    return created;
  }

  async function toggleSave() {
    if (!paper || isSaving) return;
    setIsSaving(true);
    try {
      if (isSaved) {
        const containing = shelves.filter((shelf) => shelf.paper_ids.includes(paper.id));
        setShelves((current) =>
          current.map((shelf) =>
            containing.some((c) => c.id === shelf.id)
              ? { ...shelf, paper_ids: shelf.paper_ids.filter((pid) => pid !== paper.id) }
              : shelf,
          ),
        );
        await Promise.all(containing.map((shelf) => apiClient.delete(`/shelves/${shelf.id}/papers/${paper.id}`)));
      } else {
        const shelf = await ensureDefaultShelf();
        setShelves((current) =>
          current.map((s) => (s.id === shelf.id ? { ...s, paper_ids: [...s.paper_ids, paper.id] } : s)),
        );
        await apiClient.post(`/shelves/${shelf.id}/papers/${paper.id}`);
      }
    } catch {
      await load();
    } finally {
      setIsSaving(false);
    }
  }

  function addToShelf(shelfId: string) {
    if (!paper) return;
    const shelf = shelves.find((s) => s.id === shelfId);
    if (!shelf || shelf.paper_ids.includes(paper.id)) return;
    setShelves((current) =>
      current.map((s) => (s.id === shelfId ? { ...s, paper_ids: [...s.paper_ids, paper.id] } : s)),
    );
    apiClient.post(`/shelves/${shelfId}/papers/${paper.id}`).catch(() => {
      setShelves((current) =>
        current.map((s) => (s.id === shelfId ? { ...s, paper_ids: s.paper_ids.filter((pid) => pid !== paper.id) } : s)),
      );
    });
  }

  async function citePaper() {
    if (!paper || isCiting) return;
    setIsCiting(true);
    try {
      const result = await apiClient.post<Citation>(`/papers/${paper.id}/cite`);
      const cited = { ...paper, citation_count: result.citation_count };
      setPaper(cited);
      Alert.alert('Citation recorded', formatCitation(cited));
    } catch (err) {
      Alert.alert('Could not cite paper', err instanceof ApiError ? err.message : 'Please try again.');
    } finally {
      setIsCiting(false);
    }
  }

  async function generateReview() {
    if (!paper || reviewState === 'loading') return;
    setReviewState('loading');
    setReviewError(null);
    try {
      const body: ReviewRequest = { paper_id: paper.id };
      const result = await apiClient.post<AiReviewReport>('/papers/review', body);
      setReview(result);
      setReviewState('idle');
    } catch (err) {
      setReviewState('error');
      setReviewError(err instanceof ApiError ? err.message : 'Could not generate an AI review.');
    }
  }

  function toggleReviewExpanded() {
    const next = !isReviewExpanded;
    setIsReviewExpanded(next);
    if (next && !review && reviewState !== 'loading') {
      generateReview();
    }
  }

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-academic-paper dark:bg-[#15151A]">
        <Stack.Screen options={{ title: 'Paper Details' }} />
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }

  if (error || !paper) {
    return (
      <SafeAreaView className="flex-1 items-center justify-center gap-3 bg-academic-paper px-8 dark:bg-[#15151A]">
        <Stack.Screen options={{ title: 'Paper Details' }} />
        <Text className="text-center text-base text-academic-muted dark:text-[#A6A6AC]">
          {error ?? 'Paper not found.'}
        </Text>
      </SafeAreaView>
    );
  }

  return (
    <View className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <Stack.Screen
        options={{
          title: 'Paper Details',
          headerRight: () => (
            <View className="flex-row items-center gap-4">
              {isSaved ? (
                <Pressable onPress={() => setShowShelfPicker(true)} hitSlop={8}>
                  <Folder size={20} color={colors.text} />
                </Pressable>
              ) : null}
              <Pressable onPress={toggleSave} disabled={isSaving} hitSlop={8}>
                {isSaved ? (
                  <BookmarkCheck size={22} color={colors.accent} />
                ) : (
                  <Bookmark size={22} color={colors.text} />
                )}
              </Pressable>
            </View>
          ),
        }}
      />

      <ScrollView contentContainerClassName="gap-1 p-4 pb-8">
        <Text className="font-serif text-2xl font-bold text-academic-ink dark:text-[#EFEAE0]">{paper.title}</Text>

        <View className="mt-2 flex-row items-center justify-between">
          <View className="flex-1 pr-3">
            {paper.doi ? (
              <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]" style={{ fontFamily: 'monospace' }}>
                DOI: {paper.doi}
              </Text>
            ) : null}
            <Text className="text-sm font-semibold text-academic-ink dark:text-[#EFEAE0]">
              {[paper.journal, String(paper.year)].filter(Boolean).join(' • ')}
            </Text>
          </View>
          <View className="rounded-full bg-academic-gold/20 px-3 py-1.5">
            <Text className="text-xs font-bold text-academic-navy dark:text-[#5D82AE]">
              {paper.citation_count} citation{paper.citation_count === 1 ? '' : 's'}
            </Text>
          </View>
        </View>

        {paper.ai_score !== null ? (
          <View className="mt-3 flex-row items-center gap-2">
            <Text className="text-xs font-semibold text-academic-muted dark:text-[#A6A6AC]">Readiness score</Text>
            <View className="h-1.5 flex-1 overflow-hidden rounded-full bg-academic-gold/15">
              <View
                className="h-1.5 rounded-full bg-academic-navy dark:bg-[#5D82AE]"
                style={{ width: `${Math.max(0, Math.min(100, paper.ai_score))}%` }}
              />
            </View>
            <Text className="text-xs font-bold text-academic-ink dark:text-[#EFEAE0]">{paper.ai_score}</Text>
          </View>
        ) : null}

        <View className="mt-4">
          <AiReviewCard
            reviewState={reviewState}
            review={review}
            error={reviewError}
            isExpanded={isReviewExpanded}
            onToggle={toggleReviewExpanded}
            onRetry={generateReview}
          />
        </View>

        <View className="mt-5">
          <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">Abstract</Text>
          <View className="mt-2">
            <ScholarlyText text={paper.abstract || 'No abstract available.'} style={{ fontSize: 14 }} color={colors.text} />
          </View>
        </View>

        <View className="mt-6 flex-col gap-3">
          <View className="flex-row gap-3">
            <Pressable
              onPress={citePaper}
              disabled={isCiting}
              className="flex-1 flex-row items-center justify-center gap-2 rounded-xl bg-academic-maroon px-4 py-3 dark:bg-[#C6635E]"
            >
              {isCiting ? <ActivityIndicator size="small" color="#FBF9F4" /> : <Quote size={16} color="#FBF9F4" />}
              <Text className="text-sm font-semibold text-academic-paper">Cite Paper</Text>
            </Pressable>

            <Pressable
              onPress={() => paper.pdf_url && setShowPdf(true)}
              disabled={!paper.pdf_url}
              className={`flex-1 flex-row items-center justify-center gap-2 rounded-xl border px-4 py-3 ${
                paper.pdf_url
                  ? 'border-academic-gold/40 bg-academic-parchment dark:border-academic-gold/30 dark:bg-[#1F1F26]'
                  : 'border-academic-gold/15 bg-academic-parchment/50 opacity-50 dark:border-[#33333D] dark:bg-[#1F1F26]/50'
              }`}
            >
              <FileText size={16} color={colors.accent} />
              <Text className="text-sm font-semibold text-academic-ink dark:text-[#EFEAE0]">
                {paper.pdf_url ? 'Read PDF' : 'No PDF'}
              </Text>
            </Pressable>
          </View>

          <Pressable
            onPress={() => router.push({ pathname: '/citation-graph', params: { paper_id: paper.id, initialMode: 'citations' } })}
            className="flex-row items-center justify-center gap-2 rounded-xl border border-academic-gold/30 bg-academic-gold/10 py-3 dark:border-academic-gold/20 dark:bg-[#252530]"
          >
            <Sparkles size={16} color={colors.accent} />
            <Text className="text-sm font-bold text-academic-ink dark:text-[#EFEAE0]">Explore Citation Lineage Graph</Text>
          </Pressable>
        </View>

      </ScrollView>

      <PdfViewerModal visible={showPdf} pdfUrl={paper.pdf_url} onClose={() => setShowPdf(false)} />

      <ShelfPickerModal
        visible={showShelfPicker}
        shelves={shelves}
        paperId={paper.id}
        onClose={() => setShowShelfPicker(false)}
        onAddToShelf={addToShelf}
        onShelfCreated={(shelf) => setShelves((current) => [...current, shelf])}
      />
    </View>
  );
}

function AiReviewCard({
  reviewState,
  review,
  error,
  isExpanded,
  onToggle,
  onRetry,
}: {
  reviewState: 'idle' | 'loading' | 'error';
  review: AiReviewReport | null;
  error: string | null;
  isExpanded: boolean;
  onToggle: () => void;
  onRetry: () => void;
}) {
  const { colors } = useAppTheme();

  return (
    <View className="overflow-hidden rounded-2xl border border-academic-gold/15 bg-white dark:border-[#33333D] dark:bg-[#1F1F26]">
      <Pressable onPress={onToggle} className="flex-row items-center justify-between p-4">
        <View className="flex-row items-center gap-3">
          <View className="h-9 w-9 items-center justify-center rounded-xl bg-academic-navy dark:bg-[#5D82AE]">
            <Sparkles size={17} color="#FBF9F4" />
          </View>
          <View>
            <Text className="text-sm font-bold text-academic-ink dark:text-[#EFEAE0]">AI Review Summary</Text>
            <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">Powered by Fireworks AI</Text>
          </View>
        </View>
        <ChevronDown
          size={18}
          color={colors.textSecondary}
          style={{ transform: [{ rotate: isExpanded ? '180deg' : '0deg' }] }}
        />
      </Pressable>

      {isExpanded ? (
        <View className="border-t border-academic-gold/15 p-4 dark:border-[#33333D]">
          {reviewState === 'loading' ? (
            <View className="items-center gap-2 py-4">
              <ActivityIndicator color={colors.accent} />
              <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">Generating review…</Text>
            </View>
          ) : reviewState === 'error' ? (
            <View className="flex-row items-center gap-2 py-2">
              <TriangleAlert size={18} color="#B3261E" />
              <Text className="flex-1 text-sm text-academic-ink dark:text-[#EFEAE0]">
                {error ?? "Couldn't generate a review"}
              </Text>
              <Pressable onPress={onRetry}>
                <Text className="text-sm font-bold" style={{ color: colors.accent }}>
                  Retry
                </Text>
              </Pressable>
            </View>
          ) : review ? (
            <View className="gap-4">
              <View className="flex-row items-center justify-between">
                <View className="flex-row items-center gap-2">
                  <Text className="text-2xl font-bold text-academic-ink dark:text-[#EFEAE0]">
                    {review.overall_score}
                  </Text>
                  <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">/ 100</Text>
                </View>
                {review.verdict ? (
                  <View
                    className="rounded-full px-3 py-1"
                    style={{ backgroundColor: `${VERDICT_COLORS[review.verdict] ?? colors.accent}22` }}
                  >
                    <Text
                      className="text-xs font-bold"
                      style={{ color: VERDICT_COLORS[review.verdict] ?? colors.accent }}
                    >
                      {review.verdict.replaceAll('_', ' ')}
                    </Text>
                  </View>
                ) : null}
              </View>

              <View className="flex-row flex-wrap gap-x-4 gap-y-2">
                <ScoreBar label="Structure" value={review.structure} />
                <ScoreBar label="Citations" value={review.citations} />
                <ScoreBar label="Clarity" value={review.clarity} />
                <ScoreBar label="Originality" value={review.originality} />
              </View>

              {review.summary ? (
                <Text className="text-sm leading-5 text-academic-ink dark:text-[#EFEAE0]">{review.summary}</Text>
              ) : null}

              {review.suggestions.length > 0 ? (
                <View className="gap-2">
                  <Text className="text-xs font-bold uppercase text-academic-muted dark:text-[#A6A6AC]">
                    Suggestions
                  </Text>
                  {review.suggestions.map((suggestion) => (
                    <View key={suggestion.id} className="flex-row items-start gap-2">
                      <View
                        className="mt-0.5 rounded px-1.5 py-0.5"
                        style={{ backgroundColor: `${SEVERITY_COLORS[suggestion.severity] ?? colors.accent}22` }}
                      >
                        <Text
                          className="text-[10px] font-bold"
                          style={{ color: SEVERITY_COLORS[suggestion.severity] ?? colors.accent }}
                        >
                          {SEVERITY_LABELS[suggestion.severity] ?? suggestion.severity}
                        </Text>
                      </View>
                      <Text className="flex-1 text-sm text-academic-ink dark:text-[#EFEAE0]">
                        <Text className="font-semibold">{suggestion.section}: </Text>
                        {suggestion.text}
                      </Text>
                    </View>
                  ))}
                </View>
              ) : null}
            </View>
          ) : null}
        </View>
      ) : null}
    </View>
  );
}

function ScoreBar({ label, value }: { label: string; value: number }) {
  return (
    <View style={{ width: '46%' }} className="gap-1">
      <View className="flex-row items-center justify-between">
        <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">{label}</Text>
        <Text className="text-xs font-bold text-academic-ink dark:text-[#EFEAE0]">{value}</Text>
      </View>
      <View className="h-1.5 overflow-hidden rounded-full bg-academic-gold/15">
        <View
          className="h-1.5 rounded-full bg-academic-navy dark:bg-[#5D82AE]"
          style={{ width: `${Math.max(0, Math.min(100, value))}%` }}
        />
      </View>
    </View>
  );
}

function ShelfPickerModal({
  visible,
  shelves,
  paperId,
  onClose,
  onAddToShelf,
  onShelfCreated,
}: {
  visible: boolean;
  shelves: Shelf[];
  paperId: string;
  onClose: () => void;
  onAddToShelf: (shelfId: string) => void;
  onShelfCreated: (shelf: Shelf) => void;
}) {
  const { colors } = useAppTheme();
  const [isCreating, setIsCreating] = useState(false);
  const [newShelfName, setNewShelfName] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function createShelf() {
    if (!newShelfName.trim() || isSubmitting) return;
    setIsSubmitting(true);
    try {
      const body: ShelfCreate = { name: newShelfName.trim() };
      const created = await apiClient.post<Shelf>('/shelves', body);
      onShelfCreated(created);
      onAddToShelf(created.id);
      setNewShelfName('');
      setIsCreating(false);
    } catch {
      // Leave the form open so the user can retry.
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <View className="flex-1 bg-academic-paper dark:bg-[#15151A]">
        <SafeAreaView className="flex-1">
          <View className="flex-row items-center justify-between border-b border-academic-gold/15 px-4 py-3 dark:border-[#33333D]">
            <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">Add to Shelf</Text>
            <Pressable onPress={onClose} hitSlop={8}>
              <X size={22} color={colors.text} />
            </Pressable>
          </View>

          <ScrollView contentContainerClassName="gap-2 p-4">
            {shelves.map((shelf) => {
              const containsPaper = shelf.paper_ids.includes(paperId);
              return (
                <Pressable
                  key={shelf.id}
                  onPress={() => onAddToShelf(shelf.id)}
                  disabled={containsPaper}
                  className="flex-row items-center justify-between rounded-xl border border-academic-gold/15 bg-white px-4 py-3 dark:border-[#33333D] dark:bg-[#15151A]"
                >
                  <Text className="text-sm font-semibold text-academic-ink dark:text-[#EFEAE0]">{shelf.name}</Text>
                  {containsPaper ? <Check size={18} color={colors.accent} /> : null}
                </Pressable>
              );
            })}

            {isCreating ? (
              <View className="mt-2 gap-2 rounded-xl border border-dashed border-academic-gold/30 p-3">
                <TextInput
                  value={newShelfName}
                  onChangeText={setNewShelfName}
                  placeholder="Shelf name"
                  placeholderTextColor={colors.textSecondary}
                  autoFocus
                  className="rounded-lg border border-academic-gold/20 bg-white px-3 py-2 text-sm text-academic-ink dark:border-[#33333D] dark:bg-[#1F1F26] dark:text-[#EFEAE0]"
                />
                <Button label="Create & Add" isLoading={isSubmitting} disabled={!newShelfName.trim()} onPress={createShelf} />
              </View>
            ) : (
              <Pressable
                onPress={() => setIsCreating(true)}
                className="mt-2 flex-row items-center justify-center gap-2 rounded-xl border border-dashed border-academic-gold/30 py-3"
              >
                <Plus size={16} color={colors.accent} />
                <Text className="text-sm font-semibold text-academic-ink dark:text-[#EFEAE0]">New Shelf</Text>
              </Pressable>
            )}
          </ScrollView>
        </SafeAreaView>
      </View>
    </Modal>
  );
}
