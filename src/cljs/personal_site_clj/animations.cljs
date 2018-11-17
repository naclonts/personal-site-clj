(ns personal-site-clj.animations
	(:require [quil.core :as q :include-macros true]
						[quil.middleware :as m]
						[personal-site-clj.tree-animation :as tree]
            [personal-site-clj.cities-graph :as cities]))

;; Animation code is in the tree-animation and cities-graph
;; namespaces, which are triggered by requiring above.

(println "Hello 42")
; @TODO import json data and draw it. Check out:
; https://github.com/JulianBirch/cljs-ajax



