-- ============================================================================
-- CONNECT SOCIAL MESSAGING PLATFORM – COMPLETE INSFORGE POSTGRESQL BACKEND SCHEMA
-- ============================================================================
-- Tagline: Connect people, everywhere.
-- Target Backend: InsForge (PostgreSQL + Auth + Storage + Realtime)
-- Instructions: Run this entire script inside your InsForge SQL Editor.
-- ============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. PROFILES TABLE (Linked to auth.users)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    avatar_url TEXT,
    bio TEXT DEFAULT 'Hey there! I am using CONNECT.',
    is_online BOOLEAN DEFAULT false,
    last_seen TIMESTAMPTZ DEFAULT now(),
    role TEXT DEFAULT 'user' CHECK (role IN ('user', 'admin')),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Index for fast lookup by username and search
CREATE INDEX IF NOT EXISTS idx_profiles_username ON public.profiles(username);
CREATE INDEX IF NOT EXISTS idx_profiles_is_online ON public.profiles(is_online);

-- ============================================================================
-- 2. CHATS TABLE (Direct & Group Conversations)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.chats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type TEXT NOT NULL CHECK (type IN ('direct', 'group')),
    name TEXT,
    avatar_url TEXT,
    description TEXT,
    admin_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_chats_type ON public.chats(type);

-- ============================================================================
-- 3. CHAT MEMBERS TABLE (Participants in direct & group chats)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.chat_members (
    chat_id UUID REFERENCES public.chats(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    role TEXT DEFAULT 'member' CHECK (role IN ('member', 'admin')),
    joined_at TIMESTAMPTZ DEFAULT now(),
    last_read_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (chat_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_members_user ON public.chat_members(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_members_chat ON public.chat_members(chat_id);

-- ============================================================================
-- 4. MESSAGES TABLE (Real-time Messaging)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id UUID NOT NULL REFERENCES public.chats(id) ON DELETE CASCADE,
    sender_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    type TEXT DEFAULT 'text' CHECK (type IN ('text', 'image', 'video', 'audio', 'document')),
    media_url TEXT,
    audio_duration_sec INTEGER DEFAULT 0,
    reply_to UUID REFERENCES public.messages(id) ON DELETE SET NULL,
    is_edited BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    status TEXT DEFAULT 'sent' CHECK (status IN ('sending', 'sent', 'delivered', 'read')),
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON public.messages(chat_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON public.messages(sender_id);

-- ============================================================================
-- 5. MESSAGE REACTIONS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.message_reactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    emoji TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(message_id, user_id, emoji)
);

CREATE INDEX IF NOT EXISTS idx_reactions_message ON public.message_reactions(message_id);

-- ============================================================================
-- 6. FRIENDSHIPS & CONTACTS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.friendships (
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'accepted' CHECK (status IN ('pending', 'accepted', 'blocked')),
    created_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (user_id, friend_id)
);

CREATE INDEX IF NOT EXISTS idx_friendships_user ON public.friendships(user_id);

-- ============================================================================
-- 7. STORIES & STATUS (24-Hour Expiration)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.stories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    type TEXT DEFAULT 'text' CHECK (type IN ('text', 'image', 'video')),
    media_url TEXT,
    caption TEXT,
    background_hex BIGINT DEFAULT 4278216447, -- Default CONNECT Blue (#0066FF)
    created_at TIMESTAMPTZ DEFAULT now(),
    expires_at TIMESTAMPTZ DEFAULT (now() + interval '24 hours')
);

CREATE INDEX IF NOT EXISTS idx_stories_user_expires ON public.stories(user_id, expires_at);

-- Story Viewers Table
CREATE TABLE IF NOT EXISTS public.story_views (
    story_id UUID NOT NULL REFERENCES public.stories(id) ON DELETE CASCADE,
    viewer_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    viewed_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (story_id, viewer_id)
);

-- ============================================================================
-- 8. CALLS TABLE (Voice & Video Logs & Signaling)
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.calls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caller_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('voice', 'video')),
    status TEXT DEFAULT 'ongoing' CHECK (status IN ('ongoing', 'completed', 'missed', 'rejected')),
    duration_seconds INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_calls_participants ON public.calls(caller_id, receiver_id, created_at DESC);

-- ============================================================================
-- 9. NOTIFICATIONS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('message', 'call', 'friend', 'system')),
    data JSONB DEFAULT '{}'::jsonb,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON public.notifications(user_id, is_read, created_at DESC);

-- ============================================================================
-- 10. BLOCKED USERS & MODERATION REPORTS
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.blocked_users (
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (user_id, blocked_id)
);

CREATE TABLE IF NOT EXISTS public.reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    reported_user_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    reason TEXT NOT NULL,
    details TEXT,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'resolved', 'dismissed', 'suspended')),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================================================
-- 11. USER SETTINGS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS public.user_settings (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    notifications_enabled BOOLEAN DEFAULT true,
    dark_mode BOOLEAN DEFAULT false,
    privacy_last_seen TEXT DEFAULT 'everyone' CHECK (privacy_last_seen IN ('everyone', 'contacts', 'nobody')),
    read_receipts BOOLEAN DEFAULT true,
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================================================
-- 12. AUTOMATIC PROFILE CREATION TRIGGER (On InsForge Auth SignUp)
-- ============================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
    INSERT INTO public.profiles (id, username, full_name, avatar_url)
    VALUES (
        new.id,
        COALESCE(new.raw_user_meta_data->>'username', split_part(new.email, '@', 1)),
        COALESCE(new.raw_user_meta_data->>'full_name', split_part(new.email, '@', 1)),
        COALESCE(new.raw_user_meta_data->>'avatar_url', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150')
    )
    ON CONFLICT (id) DO UPDATE
    SET username = EXCLUDED.username,
        full_name = EXCLUDED.full_name;

    INSERT INTO public.user_settings (user_id)
    VALUES (new.id)
    ON CONFLICT (user_id) DO NOTHING;

    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger attached to auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- ============================================================================
-- 13. ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friendships ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.story_views ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.calls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.blocked_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_settings ENABLE ROW LEVEL SECURITY;

-- Profiles: Anyone authenticated can view; only user can update their own
CREATE POLICY "Public profiles are viewable by everyone"
    ON public.profiles FOR SELECT
    USING (true);

CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- Chats: View chats where user is a participant
CREATE POLICY "Participants can view their chats"
    ON public.chats FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.chat_members
            WHERE chat_members.chat_id = chats.id
            AND chat_members.user_id = auth.uid()
        )
    );

CREATE POLICY "Authenticated users can create chats"
    ON public.chats FOR INSERT
    WITH CHECK (auth.uid() IS NOT NULL);

-- Chat Members: Members can view member list
CREATE POLICY "Members can view chat participants"
    ON public.chat_members FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.chat_members cm
            WHERE cm.chat_id = chat_members.chat_id
            AND cm.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can join or be added to chats"
    ON public.chat_members FOR INSERT
    WITH CHECK (auth.uid() IS NOT NULL);

-- Messages: Only participants can read messages
CREATE POLICY "Chat participants can view messages"
    ON public.messages FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.chat_members
            WHERE chat_members.chat_id = messages.chat_id
            AND chat_members.user_id = auth.uid()
        )
    );

CREATE POLICY "Chat participants can insert messages"
    ON public.messages FOR INSERT
    WITH CHECK (
        auth.uid() = sender_id AND
        EXISTS (
            SELECT 1 FROM public.chat_members
            WHERE chat_members.chat_id = messages.chat_id
            AND chat_members.user_id = auth.uid()
        )
    );

CREATE POLICY "Senders can edit their own messages"
    ON public.messages FOR UPDATE
    USING (auth.uid() = sender_id);

CREATE POLICY "Senders can delete their own messages"
    ON public.messages FOR DELETE
    USING (auth.uid() = sender_id);

-- Message Reactions
CREATE POLICY "Participants can view reactions"
    ON public.message_reactions FOR SELECT
    USING (true);

CREATE POLICY "Users can add reactions"
    ON public.message_reactions FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can remove their reactions"
    ON public.message_reactions FOR DELETE
    USING (auth.uid() = user_id);

-- Stories: Visible within 24 hours
CREATE POLICY "Users can view active stories"
    ON public.stories FOR SELECT
    USING (expires_at > now());

CREATE POLICY "Users can insert their own stories"
    ON public.stories FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own stories"
    ON public.stories FOR DELETE
    USING (auth.uid() = user_id);

-- Calls: Participants can view call history
CREATE POLICY "Call participants can view their calls"
    ON public.calls FOR SELECT
    USING (auth.uid() = caller_id OR auth.uid() = receiver_id);

CREATE POLICY "Users can log calls"
    ON public.calls FOR INSERT
    WITH CHECK (auth.uid() = caller_id);

-- Notifications
CREATE POLICY "Users can view own notifications"
    ON public.notifications FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can update own notifications"
    ON public.notifications FOR UPDATE
    USING (auth.uid() = user_id);

-- Reports: Authenticated users can submit
CREATE POLICY "Users can submit reports"
    ON public.reports FOR INSERT
    WITH CHECK (auth.uid() = reporter_id);

-- User Settings
CREATE POLICY "Users can view and update their settings"
    ON public.user_settings FOR ALL
    USING (auth.uid() = user_id);

-- ============================================================================
-- 14. REALTIME REPLICATION (For live messages, typing & presence)
-- ============================================================================
-- Enable InsForge Realtime publication for tables
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
    ALTER PUBLICATION supabase_realtime ADD TABLE public.message_reactions;
    ALTER PUBLICATION supabase_realtime ADD TABLE public.profiles;
    ALTER PUBLICATION supabase_realtime ADD TABLE public.stories;
    ALTER PUBLICATION supabase_realtime ADD TABLE public.calls;
EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN undefined_object THEN NULL;
END $$;
