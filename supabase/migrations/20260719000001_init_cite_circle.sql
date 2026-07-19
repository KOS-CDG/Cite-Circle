-- =============================================================================
-- 1. CLEANUP & EXTENSIONS
-- =============================================================================
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
DROP FUNCTION IF EXISTS public.handle_new_user();

DROP TABLE IF EXISTS shelf_papers CASCADE;
DROP TABLE IF EXISTS shelves CASCADE;
DROP TABLE IF EXISTS conversation_participants CASCADE;
DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS conversations CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS comment_likes CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS post_endorsements CASCADE;
DROP TABLE IF EXISTS saved_posts CASCADE;
DROP TABLE IF EXISTS posts CASCADE;
DROP TABLE IF EXISTS paper_authors CASCADE;
DROP TABLE IF EXISTS papers CASCADE;
DROP TABLE IF EXISTS circle_members CASCADE;
DROP TABLE IF EXISTS circles CASCADE;
DROP TABLE IF EXISTS connections CASCADE;
DROP TABLE IF EXISTS followers CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS post_type CASCADE;
DROP TYPE IF EXISTS post_flair CASCADE;

-- Enums
CREATE TYPE user_role AS ENUM ('STUDENT', 'EDUCATOR', 'RESEARCHER', 'ADMIN');
CREATE TYPE post_type AS ENUM ('DISCUSSION', 'PAPER_SHARE', 'MILESTONE', 'CIRCLE_ACTIVITY');
CREATE TYPE post_flair AS ENUM ('QUESTION', 'DISCUSSION', 'PAPER_FEEDBACK', 'RESOURCE', 'NONE');

-- =============================================================================
-- 2. CORE SCHEMAS & TABLES
-- =============================================================================

-- Public Users Profiles
CREATE TABLE users (
    id VARCHAR(100) PRIMARY KEY, -- Maps to Supabase auth.users.id UUID or custom string ID
    name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(1024) DEFAULT '',
    role user_role DEFAULT 'STUDENT',
    institution VARCHAR(255) DEFAULT '',
    field_of_study VARCHAR(255) DEFAULT '',
    bio TEXT DEFAULT '',
    orcid_id VARCHAR(100) DEFAULT '',
    follower_count INT DEFAULT 0,
    following_count INT DEFAULT 0,
    citation_count INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    interests TEXT[] DEFAULT '{}'::TEXT[]
);

-- Circles (Communities)
CREATE TABLE circles (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT DEFAULT '',
    icon_emoji VARCHAR(50) DEFAULT '',
    banner_color BIGINT NOT NULL, -- packed ARGB color
    member_count INT DEFAULT 0,
    category VARCHAR(100) DEFAULT '',
    post_count INT DEFAULT 0,
    weekly_post_count INT DEFAULT 0
);

-- Papers
CREATE TABLE papers (
    id VARCHAR(100) PRIMARY KEY,
    title VARCHAR(512) NOT NULL,
    abstract TEXT DEFAULT '',
    citation_count INT DEFAULT 0,
    year INT DEFAULT 2024,
    pdf_url VARCHAR(1024),
    doi VARCHAR(255) DEFAULT '',
    circle_id VARCHAR(100) REFERENCES circles(id) ON DELETE SET NULL,
    is_published BOOLEAN DEFAULT TRUE,
    ai_score INT,
    journal VARCHAR(255) DEFAULT ''
);

-- Posts
CREATE TABLE posts (
    id VARCHAR(100) PRIMARY KEY,
    author_id VARCHAR(100) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    type post_type DEFAULT 'DISCUSSION',
    timestamp BIGINT NOT NULL, -- Epoch milliseconds matching Android Long
    endorse_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    circle_id VARCHAR(100) REFERENCES circles(id) ON DELETE SET NULL,
    attached_paper_id VARCHAR(100) REFERENCES papers(id) ON DELETE SET NULL,
    milestone_text TEXT,
    flair post_flair DEFAULT 'NONE',
    image_url VARCHAR(1024)
);

-- Comments
CREATE TABLE comments (
    id VARCHAR(100) PRIMARY KEY,
    author_id VARCHAR(100) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id VARCHAR(100) NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    like_count INT DEFAULT 0,
    parent_id VARCHAR(100) REFERENCES comments(id) ON DELETE CASCADE
);

-- =============================================================================
-- 3. INTERACTIVE & RELATIONSHIP TABLES
-- =============================================================================

-- Followers (User <-> User)
CREATE TABLE followers (
    follower_id VARCHAR(100) REFERENCES users(id) ON DELETE CASCADE,
    following_id VARCHAR(100) REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (follower_id, following_id)
);

-- Connections (User <-> User Network Request)
CREATE TABLE connections (
    user_a_id VARCHAR(100) REFERENCES users(id) ON DELETE CASCADE,
    user_b_id VARCHAR(100) REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, ACCEPTED, DECLINED
    PRIMARY KEY (user_a_id, user_b_id)
);

-- Circle Members (User <-> Circle)
CREATE TABLE circle_members (
    circle_id VARCHAR(100) REFERENCES circles(id) ON DELETE CASCADE,
    user_id VARCHAR(100) REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (circle_id, user_id)
);

-- Paper Authors (User <-> Paper Junction)
CREATE TABLE paper_authors (
    paper_id VARCHAR(100) REFERENCES papers(id) ON DELETE CASCADE,
    user_id VARCHAR(100) REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (paper_id, user_id)
);

-- Post Endorsements (Likes)
CREATE TABLE post_endorsements (
    post_id VARCHAR(100) REFERENCES posts(id) ON DELETE CASCADE,
    user_id VARCHAR(100) REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, user_id)
);

-- Saved Posts
CREATE TABLE saved_posts (
    post_id VARCHAR(100) REFERENCES posts(id) ON DELETE CASCADE,
    user_id VARCHAR(100) REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, user_id)
);

-- Comment Likes
CREATE TABLE comment_likes (
    comment_id VARCHAR(100) REFERENCES comments(id) ON DELETE CASCADE,
    user_id VARCHAR(100) REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (comment_id, user_id)
);

-- Shelves (Library Folder)
CREATE TABLE shelves (
    id VARCHAR(100) PRIMARY KEY,
    owner_id VARCHAR(100) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT DEFAULT ''
);

-- Shelf Papers (Junction)
CREATE TABLE shelf_papers (
    shelf_id VARCHAR(100) REFERENCES shelves(id) ON DELETE CASCADE,
    paper_id VARCHAR(100) REFERENCES papers(id) ON DELETE CASCADE,
    PRIMARY KEY (shelf_id, paper_id)
);

-- =============================================================================
-- 4. MESSAGING & NOTIFICATIONS
-- =============================================================================

-- Conversations
CREATE TABLE conversations (
    id VARCHAR(100) PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Conversation Participants
CREATE TABLE conversation_participants (
    conversation_id VARCHAR(100) REFERENCES conversations(id) ON DELETE CASCADE,
    user_id VARCHAR(100) REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (conversation_id, user_id)
);

-- Messages
CREATE TABLE messages (
    id VARCHAR(100) PRIMARY KEY,
    conversation_id VARCHAR(100) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id VARCHAR(100) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    attached_paper_id VARCHAR(100) REFERENCES papers(id) ON DELETE SET NULL
);

-- Notifications
CREATE TABLE notifications (
    id VARCHAR(100) PRIMARY KEY,
    type VARCHAR(50) NOT NULL, -- ENDORSEMENT, COMMENT, CONNECTION, etc.
    actor_id VARCHAR(100) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id VARCHAR(100) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    target_id VARCHAR(100) DEFAULT ''
);

-- =============================================================================
-- 5. AUTOMATED SYNC TRIGGERS (Followers, Members, Likes, Comments)
-- =============================================================================

-- Triggers for User Follower Counts
CREATE OR REPLACE FUNCTION update_user_follower_counts()
RETURNS TRIGGER AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE users SET follower_count = follower_count + 1 WHERE id = NEW.following_id;
    UPDATE users SET following_count = following_count + 1 WHERE id = NEW.follower_id;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE users SET follower_count = GREATEST(0, follower_count - 1) WHERE id = OLD.following_id;
    UPDATE users SET following_count = GREATEST(0, following_count - 1) WHERE id = OLD.follower_id;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_followers
AFTER INSERT OR DELETE ON followers
FOR EACH ROW EXECUTE FUNCTION update_user_follower_counts();

-- Triggers for Circle Member Counts
CREATE OR REPLACE FUNCTION update_circle_member_counts()
RETURNS TRIGGER AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE circles SET member_count = member_count + 1 WHERE id = NEW.circle_id;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE circles SET member_count = GREATEST(0, member_count - 1) WHERE id = OLD.circle_id;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_circle_members
AFTER INSERT OR DELETE ON circle_members
FOR EACH ROW EXECUTE FUNCTION update_circle_member_counts();

-- Triggers for Post Endorsement Counts
CREATE OR REPLACE FUNCTION update_post_endorse_counts()
RETURNS TRIGGER AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE posts SET endorse_count = endorse_count + 1 WHERE id = NEW.post_id;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE posts SET endorse_count = GREATEST(0, endorse_count - 1) WHERE id = OLD.post_id;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_post_endorsements
AFTER INSERT OR DELETE ON post_endorsements
FOR EACH ROW EXECUTE FUNCTION update_post_endorse_counts();

-- Triggers for Post Comment Counts
CREATE OR REPLACE FUNCTION update_post_comment_counts()
RETURNS TRIGGER AS $$
BEGIN
  IF (TG_OP = 'INSERT') THEN
    UPDATE posts SET comment_count = comment_count + 1 WHERE id = NEW.post_id;
  ELSIF (TG_OP = 'DELETE') THEN
    UPDATE posts SET comment_count = GREATEST(0, comment_count - 1) WHERE id = OLD.post_id;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_post_comments
AFTER INSERT OR DELETE ON comments
FOR EACH ROW EXECUTE FUNCTION update_post_comment_counts();

-- =============================================================================
-- 6. SUPABASE AUTH TRIGGERS (Automated public profile creation on sign-up)
-- =============================================================================

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.users (
    id,
    name,
    avatar_url,
    role,
    institution,
    field_of_study,
    bio,
    orcid_id,
    follower_count,
    following_count,
    citation_count,
    is_verified,
    interests
  )
  VALUES (
    new.id,
    COALESCE(new.raw_user_meta_data->>'name', split_part(new.email, '@', 1)),
    COALESCE(new.raw_user_meta_data->>'avatar_url', ''),
    'STUDENT', -- Default role for new users
    COALESCE(new.raw_user_meta_data->>'institution', ''),
    COALESCE(new.raw_user_meta_data->>'field_of_study', ''),
    '',
    '',
    0,
    0,
    0,
    FALSE,
    '{}'::TEXT[]
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- =============================================================================
-- 7. PERFORMANCE INDICES
-- =============================================================================
CREATE INDEX idx_users_interests ON users USING gin(interests);
CREATE INDEX idx_posts_timestamp ON posts(timestamp DESC);
CREATE INDEX idx_comments_post ON comments(post_id);
CREATE INDEX idx_messages_conversation ON messages(conversation_id, timestamp ASC);
CREATE INDEX idx_notifications_receiver ON notifications(receiver_id, timestamp DESC);

-- =============================================================================
-- 8. INITIAL SEED DATA (Realistic academics and circles)
-- =============================================================================

INSERT INTO users (id, name, avatar_url, role, institution, field_of_study, bio, orcid_id, citation_count, is_verified, interests) VALUES
('u0', 'Maya Okafor', 'https://api.dicebear.com/8.x/avataaars/svg?seed=maya', 'RESEARCHER', 'MIT Media Lab', 'Human-Computer Interaction', 'PhD candidate exploring situated cognition in AI-augmented workspaces. Ex-Google Brain. She/her.', '0000-0002-7834-1291', 1240, TRUE, '{"HCI", "AI", "Cognitive Science", "Design"}'),
('u1', 'Dr. Elena Reyes', 'https://api.dicebear.com/8.x/avataaars/svg?seed=elena', 'EDUCATOR', 'Stanford University', 'Computational Biology', 'Associate Professor of Computational Biology. PI of the Reyes Lab. NSF CAREER Award 2021.', '0000-0001-5523-4521', 8740, TRUE, '{"Genomics", "Machine Learning", "Proteomics"}'),
('u2', 'Prof. James Whitmore', 'https://api.dicebear.com/8.x/avataaars/svg?seed=james', 'EDUCATOR', 'Harvard University', 'Cognitive Neuroscience', 'William James Professor of Cognitive Science. Bestselling author of ''The Attention Economy of the Mind''.', '0000-0003-1234-5678', 34200, TRUE, '{"Neuroscience", "Psychology", "Decision Making"}'),
('u3', 'Aisha Nakamura', 'https://api.dicebear.com/8.x/avataaars/svg?seed=aisha', 'RESEARCHER', 'University of Tokyo', 'Quantum Computing', 'Postdoctoral researcher in quantum error correction. JST Fellowship. Interested in fault-tolerant computation.', '0000-0002-9981-3345', 3100, TRUE, '{"Quantum Computing", "Physics", "Information Theory"}'),
('u4', 'Carlos Mendoza', 'https://api.dicebear.com/8.x/avataaars/svg?seed=carlos', 'STUDENT', 'UC Berkeley', 'Machine Learning', '3rd-year PhD student in CS, advised by Prof. Jordan. Working on causal inference in large language models.', '', 245, FALSE, '{"Machine Learning", "Causality", "NLP"}');

INSERT INTO circles (id, name, description, icon_emoji, banner_color, category, post_count, weekly_post_count) VALUES
('c1', 'Machine Learning Frontiers', 'Cutting-edge ML research: architectures, theory, applications, and reproducibility challenges.', '🤖', 4282246109, 'Computer Science', 8900, 340),
('c2', 'Genomics & Precision Medicine', 'From GWAS to gene therapy: a community for genomicists and clinicians.', '🧬', 4280955814, 'Life Sciences', 5600, 218),
('c3', 'Cognitive & Behavioral Sciences', 'Bridging experimental psychology, neuroscience, and computational modeling of the mind.', '🧠', 4294937451, 'Social Sciences', 3100, 145);

-- Seed Memberships (Trigger will auto-update member_count)
INSERT INTO circle_members (circle_id, user_id) VALUES
('c1', 'u0'),
('c1', 'u4'),
('c2', 'u1'),
('c3', 'u2');

-- Seed Followers (Trigger will auto-update follower_counts)
INSERT INTO followers (follower_id, following_id) VALUES
('u0', 'u1'),
('u1', 'u0'),
('u4', 'u0'),
('u0', 'u3');

INSERT INTO papers (id, title, abstract, citation_count, year, doi, circle_id, is_published, ai_score, journal) VALUES
('p1', 'Situated Cognition in AI-Augmented Knowledge Work: A Mixed-Methods Study of Academic Researchers', 'As AI writing and analysis tools become embedded in research workflows, questions arise about cognitive load...', 48, 2024, '10.1145/3613904.3642234', 'c1', TRUE, 87, 'CHI 2024'),
('p2', 'Polygenic Risk Score Calibration Across Diverse Ancestry Groups: Lessons from the PAGE Study', 'Polygenic risk scores (PRS) derived predominantly from European ancestry GWAS cohorts exhibit reduced accuracy...', 312, 2023, '10.1038/s41588-023-01429-8', 'c2', TRUE, 94, 'Nature Genetics');

INSERT INTO paper_authors (paper_id, user_id) VALUES
('p1', 'u0'),
('p1', 'u1'),
('p2', 'u1');

INSERT INTO posts (id, author_id, content, type, timestamp, circle_id, attached_paper_id, flair) VALUES
('post1', 'u0', 'Thrilled to share our new paper on situated cognition in AI-augmented workspaces. Check it out!', 'PAPER_SHARE', 1721360000000, 'c1', 'p1', 'RESOURCE'),
('post2', 'u4', 'Has anyone tried running the PAGE study calibration weights on local clinical datasets?', 'DISCUSSION', 1721365000000, 'c1', 'p2', 'QUESTION');

-- Seed Endorsements (Trigger will auto-update endorse_count)
INSERT INTO post_endorsements (post_id, user_id) VALUES
('post1', 'u4'),
('post1', 'u1');

-- Seed Comments (Trigger will auto-update comment_count)
INSERT INTO comments (id, author_id, post_id, content, timestamp) VALUES
('cm1', 'u1', 'post1', 'Excellent framework. The distributed epistemic scaffolding concept is exactly what is needed.', 1721362000000);
