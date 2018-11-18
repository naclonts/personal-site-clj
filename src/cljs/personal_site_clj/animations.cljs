(ns personal-site-clj.animations
	(:require [quil.core :as q :include-macros true]
						[quil.middleware :as m]
						[personal-site-clj.tree-animation :as tree]
            [personal-site-clj.cities-graph :as cities]))

;; Animation code is in the tree-animation and cities-graph
;; namespaces, which are triggered by requiring above.

