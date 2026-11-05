# Security Specification

## 1. Data Invariants
- Users can only access their own user profile document.
- Users can only access conversations they are participating in.
- Messages can only be read/written by participants of the associated conversation.
- Email verification status is maintained on the user profile.

## 2. The "Dirty Dozen" Payloads
1. User A tries to read User B's profile.
2. User A tries to write to User B's profile.
3. User A tries to read a conversation they are not participating in.
4. User A tries to send a message to a conversation they are not participating in.
5. User A tries to modify a conversation's participants list (unauthorized).
6. User A tries to inject a massive string (1MB) into a message text field.
7. User A tries to delete a message sent by User B.
8. User A tries to set their own emailVerified to true (unauthorized).
9. User A tries to inject a ghost field (e.g., isAdmin: true) into their profile update.
10. User A tries to create a message in a non-existent conversation.
11. User A tries to create a conversation with a fake participant list.
12. User A tries to read messages from a conversation they are not part of.

## 3. The Test Runner
Tests will be implemented in `firestore.test.js` and `*RuleTest.kt` as per the Firestore skill documentation.
