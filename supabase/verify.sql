-- Verification: run after the init migration; expected values noted per column.
SELECT
  (SELECT COUNT(*) FROM users) AS total_users,                                   -- 5
  (SELECT COUNT(*) FROM circles) AS total_circles,                               -- 3
  (SELECT COUNT(*) FROM papers) AS total_papers,                                 -- 2
  (SELECT COUNT(*) FROM posts) AS total_posts,                                   -- 2
  (SELECT COUNT(*) FROM comments) AS total_comments,                             -- 1
  (SELECT follower_count FROM users WHERE id = 'u1') AS dr_elena_followers,      -- 1 (followers trigger)
  (SELECT member_count FROM circles WHERE id = 'c1') AS ml_circle_members,       -- 2 (membership trigger)
  (SELECT endorse_count FROM posts WHERE id = 'post1') AS post1_endorsements;    -- 2 (endorsements trigger)
