(ns personal-site-clj.components.ui
  (:require [com.stuartsierra.component :as component]
            [personal-site-clj.core :refer [render]]))

(defrecord UIComponent []
  component/Lifecycle
  (start [component]
    (render)
    component)
  (stop [component]
    component))

(defn new-ui-component []
  (map->UIComponent {}))
