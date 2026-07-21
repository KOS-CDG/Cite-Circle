import { useEffect, useState } from 'react';

import { apiClient } from './api-client';
import type { User } from '@/types';

/**
 * Posts/comments only carry author_id — the backend never embeds the author
 * object (unlike the Kotlin app's local repository layer) — so every card
 * needs its own GET /users/{id}. This cache dedupes concurrent lookups for
 * the same id and keeps resolved users around for the session.
 */
const cache = new Map<string, User>();
const inFlight = new Map<string, Promise<User>>();
const listeners = new Set<() => void>();

function notify() {
  listeners.forEach((listener) => listener());
}

function fetchUser(id: string): Promise<User> {
  const cached = cache.get(id);
  if (cached) return Promise.resolve(cached);

  const pending = inFlight.get(id);
  if (pending) return pending;

  const request = apiClient
    .get<User>(`/users/${id}`)
    .then((user) => {
      cache.set(id, user);
      inFlight.delete(id);
      notify();
      return user;
    })
    .catch((err) => {
      inFlight.delete(id);
      throw err;
    });

  inFlight.set(id, request);
  return request;
}

/** Returns the cached user for `id`, triggering a background fetch on first access. */
export function useUserById(id: string | null | undefined): User | undefined {
  const [, forceRender] = useState(0);

  useEffect(() => {
    if (!id || cache.has(id)) return;
    fetchUser(id).catch(() => {});
  }, [id]);

  useEffect(() => {
    const listener = () => forceRender((n) => n + 1);
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  }, []);

  return id ? cache.get(id) : undefined;
}

export function primeUserCache(user: User) {
  cache.set(user.id, user);
  notify();
}
