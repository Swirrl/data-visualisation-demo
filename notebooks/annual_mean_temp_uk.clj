(ns notebooks.annual-mean-temp-uk
  (:require [clojure.data.csv :as csv]
            [nextjournal.clerk :as clerk]
            [aerial.hanami.common :as hc]
            [aerial.hanami.templates :as ht]))

;; Evaluate the forms inside this comment to show your notebook in a browser

(comment
  (require '[nextjournal.clerk :as clerk])
  (clerk/serve! {:watch-paths ["."] :browse? true})
  (clerk/show! "notebooks/annual_mean_temp_uk.clj"))

;; # Visualising Data in Clojure with Hanami

;; **Where we left off last time:**

(-> "data/annual-mean-temp-uk.csv" ;; name of the file
    slurp                          ;; read the contents of the file
    csv/read-csv                   ;; parse the result as csv data
    clerk/use-headers              ;; tell Clerk to use the first row as headers
    clerk/table)

;; ## A brief background on Grammar for Graphics

;; Vega and vega-lite are libraries for declaratively describing data visualisations. Like
;; regular languages, they can be thought of as having words and rules for combining those words.

;; Vega-lite is built on top of Vega and is effectively a more concise version of the same thing
;; that abstracts away some of the details that are common to most data visualisations. We'll be
;; working with vega for the rest of this tutorial.

;; The main components of the Vega-lite "language" are described below:
;;   - `data`: input for the visualisation
;;   - `mark`: shape/type of graphics to use
;;   - `encoding`: mapping between data and marks
;;   - `transform`: e.g. filter, aggregate, bin, etc.
;;   - `scale`: meta-info about how the data should fit into the visualisation
;;   - `guides`: legends, labels

;; These are some of the available JSON keys in a Vega-lite spec. The corresponding values are the
;; "rules" for combining these "words". Some examples are concat, layer, repeat, facet, resolve.
;; There is more to Vega-lite, but this is enough to get started and to build the visualisation
;; for our project. For many more examples of how this grammar is used to piece together "sentences"
;; that Vega-lite can interpret and render as a graphic, see the [Vega-lite wesbsite](https://vega.github.io/vega-lite/examples/).

;; ## Rendering a Vega spec in Clerk

;; Now we can start exploring a bit in our notebook. As mentioned above, a Vega-lite spec is just
;; some JSON data that uses specific, supported keys and values. Clojure can work with JSON, but
;; conventionally uses EDN as a data transfer format. The "E" stands for "extensible", so it supports
;; more complex data structures than JSON does, meaning all JSON can be directly converted to EDN,
;; but not the other way around. This is relevant because our Clerk notebook will expect Vega-lite
;; specifications to be written in EDN, not JSON.

;; In order to get started, and see what Clerk does with a Vega spec, we can copy a simple example
;; directly from the Vega-lite website. As an example, copy-pasting [this Vega-lite specification](https://vega.github.io/vega-lite/examples/layer_line_co2_concentration.html#spec)
;; for a visualisation of carbon dioxide in the atmosphere over time into a [JSON to EDN converter](http://cljson.com)
;; online gives us an EDN version of the specification.

;; For now, paste the EDN result into the notebook that we started last time. For a reminder about
;; how to render the Clerk notebook in your browser, see the previous post.

;; _Here is the vega-lite spec, copied verbatim from the vega-lite demo
;; (https://vega.github.io/vega-lite/examples/layer_line_co2_concentration.html#spec)
;; and converted to EDN:_

;; _It's also assigned to the var `co2-spec` to simplify using it later_

(def co2-spec-vega-demo
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :data {:url "data/co2-concentration.csv"
          :format {:parse {:Date "utc:'%Y-%m-%d'"}}}
   :width 800
   :height 500
   :transform [{:calculate "year(datum.Date)" :as "year"}
               {:calculate "floor(datum.year / 10)" :as "decade"}
               {:calculate "(datum.year % 10) + (month(datum.Date)/12)"
                :as "scaled_date"}
               {:calculate "datum.first_date === datum.scaled_date ? 'first' : datum.last_date === datum.scaled_date ? 'last' : null"
                :as "end"}]
   :encoding {:x {:type "quantitative" :title "Year into Decade" :axis {:tickCount 11}}
              :y {:title "CO2 concentration in ppm" :type "quantitative" :scale {:zero false}}
              :color {:field "decade" :type "ordinal" :scale {:scheme "magma"} :legend nil}}
   :layer [{:mark "line" :encoding {:x {:field "scaled_date"} :y {:field "CO2"}}}
           {:mark {:type "text" :baseline "top" :aria false}
            :encoding {:x {:aggregate "min" :field "scaled_date"}
                       :y {:aggregate {:argmin "scaled_date"} :field "CO2"}
                       :text {:aggregate {:argmin "scaled_date"} :field "year"}}}
           {:mark {:type "text" :aria false}
            :encoding {:x {:aggregate "max" :field "scaled_date"}
                       :y {:aggregate {:argmax "scaled_date"} :field "CO2"}
                       :text {:aggregate {:argmax "scaled_date"} :field "year"}}}]
   :config {:text {:align "left" :dx 3 :dy 1}}})

;; Your copy-pasted EDN from the JSON conversion might appear more verbose, depending on
;; how much whitespace is added for readability. Note that commas are optional in Clojure
;; and they are interpreted as whitespace by the reader; exactly the same as spaces, tabs, and newlines.

;; To get Clerk to render this Vega-lite spec, we just need to pass it as the only argument
;; to Clerk's Vega viewer, `clerk/vl`:

(clerk/vl co2-spec-vega-demo)

;; Note that it fails because the data source is specified as a relative path.
;; We need to update the value of the `:data` > `:url` key to make it render:

(def co2-spec
  (assoc-in co2-spec-vega-demo [:data :url]
            "https://vega.github.io/vega-lite/data/co2-concentration.csv"))

(clerk/vl co2-spec)

;; This spec is quite elaborate, but a useful demonstration of how all the components of
;; Vega-lite come together to describe a data visualisation.

;; ## Hanami Setup

;; The next (and final) topic we'll cover is Hanami. As you can see, Vega-lite specs can
;; quickly become very verbose, so Hanami is a Clojure library that was developed to simplify
;; writing these Vega specs. Using Hanami we can write compact, declarative, composable, and
;; recursively parameterised visualisation templates.

;; _Note: Hanami has already been added as a dependency to this project and the relevant namespaces
;; required in this notebook. See `deps.edn` and the namespace declaration above._

;; ### Hanami Background

;; In Hanami, a "template" is just a Clojure map of substitution keys. The library defines
;; many default substitution keys and a set of base templates to use as starting points,
;; which you can see by entering `@hc/_defaults` in a REPL. This will print a big map with
;; all of the default values that Hanami uses in the templates:

@hc/_defaults


;; By convention, Hanami substitution keys are all-caps symbols. To get the default value
;; for a specific substitution key, Hanami includes a helper function `hc/get-default`.
;; For example, to find the default value Hanami uses for all visualisation backgrounds, we can run:

(hc/get-default :BACKGROUND)

;; The main function in Hanami is `hc/xform`. This is the transformation function that takes a
;; template and optionally a series of extra transformation key/value pairs, inserting the values
;; wherever the keys are specified in the template. If no key/value pairs are provided, Hanami
;; uses the built-in defaults. To see what all this means, we can use Hanami to fill in a very basic
;; template (remember, a template is just a Clojure map where the values are Hanami substitution keys,
;; which are just all-caps symbols, some of which have default values):

(hc/xform {:my-key :BACKGROUND})

;; If we were to supply a custom value for :BACKGROUND, Hanami would use that instead, for example:

(hc/xform {:my-key :BACKGROUND} :BACKGROUND "orange")

;; Transformations are recursive, i.e. the definitions of substitution keys can themselves contain
;; substitution keys. For example, inspect the default value for `:TOOLTIP`:

(hc/get-default :TOOLTIP)

;; The result includes several values which are themselves substitution keys. When used in a
;; transformation and given no extra parameters, all of these values are replaced with the Hanami defaults:

(hc/xform {:my-key :TOOLTIP})

;; If we supply any custom values to override these defaults, they will be used in the transformation:

(hc/xform {:my-key :TOOLTIP} :X "my custom x value")

;; This is a good time to note Hanami's special "nothing" value, `RMV`. Notice in the above transformed
;; `:TOOLTIP` example that several of the keys that were specified in its definition have been removed.
;; Inspecting the value of `hc/RMV` reveals that it's just an alias for the special "nothing" value of
;; the underlying library which Hanami uses for data manipulation (which is called [Specter](https://github.com/redplanetlabs/specter)):

hc/RMV

;; If we check, for example the default value for `:XTITLE`:

(hc/get-default :XTTITLE)

;; we can see it's the same thing. This is a useful shortcut in Hanami to specify a default of "nothing"
;; but still include a given key in a template, which can be helpful to demonstrate to users which keys
;; are available and for writing generic, composable templates. Vega-lite does not gracefully ignore null
;; values in specs, so we have to remove them before trying to render a visualisation.

;; The last relevant piece of background to understand in order to get started with Hanami is its built-in
;; templates. The `aerial.hanami.templates` namespace defines several common Vega-lite chart templates
;; and other useful composable chart components. See, for example:

ht/bar-chart

;; or

ht/point-chart

;; ## Putting it all together

;; We finally have all the necessary pieces to visualise our dataset from last time.
;; To get started we can use Hanami's built-in `line-chart` template. To render the skeleton
;; of the chart, we can use Hanami to transform a bare-bones line chart template into a Vega-lite
;; spec and tell Clerk to render it:

(-> (hc/xform ht/line-chart)
    clerk/vl)

;; Next, we need to supply our data to the graph. Vega-lite expects [data to be in a tabular format](https://vega.github.io/vega-lite/docs/data.html),
;; like a spreadsheet or database table. In JSON or EDN format this means the data looks like a
;; collection of maps. Hanami will automatically convert data in CSV format to a vector of maps for us.

;; Hanami supports many [different ways of specifying the data source](https://github.com/jsa-aerial/hanami#data-sources).
;; We can point Hanami to the data that's already in a file in our project using the `:FDATA` substitution key (`F` for file):

(-> (hc/xform ht/line-chart
              :FDATA "data/annual-mean-temp-uk.csv")
    clerk/vl)

;; We also need to tell Vega-lite which columns to use for the x and y axes:

(-> (hc/xform ht/line-chart
              :FDATA "data/annual-mean-temp-uk.csv"
              :X "Year"
              :Y "Annual Mean Temperature")
    clerk/vl)

;; Aaaaaaaaaad we finally have some visualised data!

;; From here we can make some minor improvements. The effect of each extra value is explained in
;; the comment beside it. This tidies up our graph to something we can more easily interpret:


(-> (hc/xform ht/line-chart
              :FDATA "data/annual-mean-temp-uk.csv"
              :X "Year"
              :Y "Annual Mean Temperature"
              :XTITLE "Year" ;; label for the x axis
              :YTITLE "Average temperature (ºC)" ;; label for the y axis
              :XTYPE "temporal" ;; display x axis labels as years, not numbers
              :YSCALE {:zero false} ;; don't force the baseline to be zero
              :WIDTH 700 ;; make the graph wider
              )
    clerk/vl)

;; Vega-lite has many built-in transformations, including [locally-estimated scatterplot smoothing](https://vega.github.io/vega-lite/docs/loess.html),
;; which can be used to produce a trend line. If we wanted to render a trend line, rather than plot each
;; point individually, we just have to tell Vega-lite which axes to use in the transformation:

(-> (hc/xform ht/line-chart
              :FDATA "data/annual-mean-temp-uk.csv"
              :X "Year"
              :Y "Annual Mean Temperature"
              :XTITLE "Year"
              :YTITLE "Average temperature (ºC)"
              :XTYPE "temporal"
              :YSCALE {:zero false}
              :WIDTH 700
              :TRANSFORM [{:loess :Y :on :X}]) ;; <<<- this is new
    clerk/vl)

;; Vega-lite also supports layering two charts on the same set of axes, so we can layer these two
;; graphs on top of each other to generate our final visualisation. Hanami includes a template to
;; simplify this, `ht/layer-chart`. I've also changed the colour for the mark on the second graph
;; by specifying a different value for `:MCOLOR`:


(-> (hc/xform ht/layer-chart
              :FDATA "data/annual-mean-temp-uk.csv"
              :LAYER [(hc/xform ht/line-chart
                                :X "Year"
                                :Y "Annual Mean Temperature"
                                :XTITLE "Year"
                                :YTITLE "Average temperature (ºC)"
                                :XTYPE "temporal"
                                :YSCALE {:zero false}
                                :WIDTH 700)
                      (hc/xform ht/line-chart
                                :X "Year"
                                :Y "Annual Mean Temperature"
                                :XTITLE "Year"
                                :YTITLE "Average temperature (ºC)"
                                :XTYPE "temporal"
                                :YSCALE {:zero false}
                                :WIDTH 700
                                :TRANSFORM [{:loess :Y :on :X}]
                                :MCOLOR "orange")])
    clerk/vl)

;; We have our Vega-lite spec being generated by Hanami and rendered with Clerk! Almost any visualisation
;; you can imagine can be written as a Vega or Vega-lite specification, and Hanami also supports much
;; more than we covered here. If you're interested in learning more, both libraries have excellent
;; documentation and [lots of examples](https://vega.github.io/vega-lite/examples/).
