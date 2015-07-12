---
title: "A powerful Clojure REPL with Vinyasa"
---

As a good member of the Lisp family, Clojure's pride is its REPL. This may seem
unusual if you're coming from another language. But the workflow popular with many
Clojure developers requires a powerful REPL as its foundation.

In essence, a REPL -- or read-eval-print loop -- is a prompt that allows you to
enter instructions to modify the state of the program, printing the results to
the terminal. On the surface, Clojure's REPL is similar to tools provided by
other dynamic languages: Python's `ipython`, Ruby's `irb`, PHP's
`boris`. However, the flexible nature of Lisp makes this simple tool more
powerful. In Clojure, you can re-evaluate functions or entire namespaces while
keeping the state of the running programming. This enables the programmer to try
out a function, make changes to different parts of the system and add code
interactively, without ever restarting the REPL.

This feature allows for a different way of working with code, and for many it's
hard to go back to a language without a powerful REPL after trying it. There's
another -- decidedly more prosaic -- reason why Clojure developers work from
inside a REPL: Clojure start-up is painfully slow. A project with a dozen of
dependencies may take 10 seconds to start, even on a fast machine. If you're
looking for quick feedback loops, as you should, your only possibility is to
keep a REPL running and to re-evaluate code on the fly, as you're working on
it. The upside is that, once you're connected to the REPL, re-evaluating is
almost instant -- and you get all the other benefits of the REPL as a bonus.

As a result of these *push* and *pull* factors, Clojure's default workflow
involves starting a REPL only once (or at least rarely) and keeping it running
for the whole development session. You start `lein repl` in your project
directory, connect using a REPL-aware editor (like CIDER or LightTable), and
work dynamically.

This works great for exploring and tinkering with your code. There is, however,
one catch: due to limitations of the JVM, `leiningen` keep your `CLASSPATH`
static throughout the session. Consequently, `leiningen`'s default configuration
requires you to restart the REPL to add Clojure (or Java) dependencies.

Fortunately, `leiningen` is extensible enough that this feature can be added
by using a wonderful library called `vinyasa`, which allows you to pull
dependencies and modify the `CLASSPATH` after the fact. To do so is surprisingly
simple: simply add a configuration file as `leiningen`'s default user
profile. As a first approximation, you can try the
[profiles.clj](https://gist.github.com/pesterhazy/0d37bfffc9d7264c3b35) that I use:

``` bash
$ mkdir -p ~/.lein
$ wget -O ~/.lein/profiles.clj https://gist.githubusercontent.com/pesterhazy/0d37bfffc9d7264c3b35/raw/f754399b79ae39380a0015b6cba85b8ef76d3c72/profiles.clj
```

With this `profiles.clj` in place, every `lein repl` you start will have a number
of additional tools available in the special `./` namespace. In particular, you
can now pull an additional dependency from *clojars* in one line of code by typing:

```
(./pull 'org.clojure/data.xml "0.0.8")
;; => {[org.clojure/clojure "1.4.0"] nil, [org.clojure/data.xml "0.0.8"] #{[org.clojure/clojure "1.4.0"]}}
```

after which you can use the library, which now magically appears in the `CLASSPATH`:

```
user=> (require '[clojure.data.xml :as xml])
nil
user=> (xml/indent-str (xml/sexp-as-element [:hello "world"]))
"<?xml version=\"1.0\" encoding=\"UTF-8\"?><hello>world</hello>\n"
```

You can even omit the version number, in which case `vinyasa` will pull in the
latest version of the library (and helpfully tell which version it retrieved).

## Further reading

- [This article](http://dev.solita.fi/2014/03/18/pimp-my-repl.html) explains
  Vinyasa and many other useful things you can do in a Clojure REPL.
