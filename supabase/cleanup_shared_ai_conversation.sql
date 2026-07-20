-- One-time cutoff for the shared AI Copilot conversation.
--
-- Context: every user's AI chat was written into a single "conv_ai" row, so all
-- messages were readable by all users. Per-user threads ("conv_ai_<user_id>") are
-- now provisioned on demand by _ensure_ai_conversation(). This script removes the
-- commingled history left behind by the old scheme.
--
-- DESTRUCTIVE AND IRREVERSIBLE: every user permanently loses their AI chat history.
-- Take a Supabase backup before running. Run the SELECTs first and confirm the
-- counts look like what you expect.

-- 1. Inspect before deleting.
SELECT count(*) AS message_count,
       count(DISTINCT sender_id) AS distinct_senders
FROM messages
WHERE conversation_id = 'conv_ai';

-- 2. Delete the commingled messages.
DELETE FROM messages
WHERE conversation_id = 'conv_ai';

-- 3. Drop the shared participant rows and the conversation itself.
--    Per-user rows are recreated on the next AI request, so nothing needs seeding.
DELETE FROM conversation_participants
WHERE conversation_id = 'conv_ai';

DELETE FROM conversations
WHERE id = 'conv_ai';

-- 4. Verify the shared thread is gone and only scoped threads remain.
SELECT conversation_id, count(*)
FROM messages
WHERE conversation_id LIKE 'conv_ai%'
GROUP BY conversation_id;
