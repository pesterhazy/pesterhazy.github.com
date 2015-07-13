---
title:  "How to mapify a sequence"
---

In a recent [tweet](https://twitter.com/stuartsierra/status/586443785606836226),
Stuart Sierra writes:

> I build maps with `reduce` & `assoc` instead of `into {} map [k v]` — must be
> a sign of a reductive personality

It's an (oblique) reference to a common problem: how do you construct a map from
a sequence of elements based on a unique key? [^yada]

This need arises frequently when you deal with `jdbc`, Java's SQL abstraction.

[^yada]: One two three, lorem ispum.