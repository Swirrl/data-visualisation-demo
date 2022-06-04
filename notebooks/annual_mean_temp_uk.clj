(ns notebooks.annual-mean-temp-uk
  (:require [clojure.data.csv :as csv]
            [nextjournal.clerk :as clerk]))

(-> "data/annual-mean-temp-uk.csv" ;; name of the file
    slurp                          ;; read the contents of the file
    csv/read-csv                   ;; parse the result as csv data
    clerk/use-headers              ;; tell Clerk to use the first row as headers
    clerk/table)                   ;; render the data as a table