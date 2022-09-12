# Data Visualisation Demo

A repo containing the code from Swirrl's blog series on data visualisation using [Hanami](https://github.com/jsa-aerial/hanami), rendered with [Clerk](https://github.com/nextjournal/clerk).

To learn how to set up a Clojure environment and Clerk notebook, see the first post in the series: https://medium.swirrl.com/exploring-data-with-clojure-and-clerk-7010ee4e9346

The `main` branch has the code up to the end of the first tutorial. The `completed-visualisation` branch contains the completed code for the entire demo.

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