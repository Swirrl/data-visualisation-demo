# Data Visualisation Demo

A repo containing the code from Swirrl's blog series on data visualisation using [Hanami](https://github.com/jsa-aerial/hanami), rendered with [Clerk](https://github.com/nextjournal/clerk).

First post in the series, about setting up a Clojure environment and Clerk notebook: https://medium.swirrl.com/exploring-data-with-clojure-and-clerk-7010ee4e9346

# Usage

Start a REPL, and run:

```clojure
(require '[nextjournal.clerk :as clerk])
(clerk/serve! {:watch-paths ["."] :browse? true})
(clerk/show! "notebooks/annual_mean_temp_uk.clj")
```

# License

Copyright © 2022 Swirrl

Distributed under the MIT License.