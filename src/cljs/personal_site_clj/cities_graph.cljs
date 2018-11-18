(ns personal-site-clj.cities-graph
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [ajax.core :refer [GET]]
            [cljs.core.async
             :as async
             :refer [>! <! go chan close!]]
            ;; Graph data type implementation
            [personal-site-clj.graph :as G]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility functions
(defn log [x]
  (.log js/console x))

(defn get-cities-data []
  "Returns channel that gives the array of city data map objects."
  (let [out (chan)]
    (let [handler
          (fn [res] (go (log res) (>! out res) (close! out)))]
      (GET "/json/cities_partial.json"
           {:handler handler
            :response-format :json
            :keywords? true}))
    out))

(defn distance-between-points
  "Returns miles between two points on the earth."
  [lat1 lon1 lat2 lon2]
  (let [r 6371e3 ;; earth's radius in meters
        p1 (q/radians lat1)
        p2 (q/radians lat2)
        delta-p (q/radians (- lat2 lat1))
        delta-l (q/radians (- lon2 lon1))]
    (let [a (+
             (q/sq (q/sin (/ delta-p 2)))
             (* (q/cos p1) (q/cos p2)
                (q/sq (q/sin (/ delta-l 2)))))
          c (* 2 (q/atan2 (q/sqrt a) (q/sqrt (- 1 a))))]
      ;; calculate and convert to miles
      (* r c 0.0006214))))

(defn distance-between-cities
  "Returns miles between two city maps."
  [city1 city2]
  (distance-between-points
   (:latitude city1) (:longitude city1)
   (:latitude city2) (:longitude city2)))

(defn palette
  [color-symbol]
  (case color-symbol
    :bg (q/color 0 0 13)
    :bg-transparent (q/color 0 0 13 1)
    :light-gray (q/color 0 0 67)
    :orange (q/color 29 100 100)
    :wispy-gray (q/color 0 0 67 50)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Application logic

(defn connect-cities?
  [city1 city2]
  (< (distance-between-cities city1 city2) 600))

(defn add-city
  "Adds a city to `g` as a vertex, including creating connections
  to neighbors."
  [city {:keys [vertices] :as graph}]
  (let [g (G/add-vertex (:city city) city graph)
        v (G/get-vertex (:city city) g)
        vertex-list (into () (seq vertices))]
    (loop [[[key u] & existing-vertices] vertex-list
           new-graph g]
      (if (nil? u)
        new-graph
        (if (connect-cities? city (:value u))
          (recur
           existing-vertices
           (G/add-edge v u new-graph))
          (recur
           existing-vertices
           new-graph))))))

(defn build-cities-graph
  "Transforms the vector of cities data into a graph."
  [data]
  (println data)
  (loop [graph (G/->Graph {})
         data (into () data)]
    (let [city (peek data)]
      (if (nil? city)
        graph
        (recur
         (add-city city graph)
         (rest data))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Drawing processes!
(def FRAME-RATE 30)
(def curtain-time 2)
(def curtain-inc (/ 1 (* curtain-time FRAME-RATE)))
(def explore-time-per-iter 0.5)
(def frames-per-explore (* explore-time-per-iter FRAME-RATE))
(def explore-iter-inc (/ 1 (* explore-time-per-iter FRAME-RATE)))

(defn next-stage
  "Based on the current sketch stage, return the next one."
  [current-stage]
  (println "stage " current-stage)
  (case current-stage
    :curtain :explore))

(def initial-data (atom {}))
(defn cities-setup []
  (q/color-mode :hsb 360 100 100)
  (q/background (q/color 0 0 0 1))
  (q/frame-rate 30)
  (let [g (build-cities-graph @initial-data)]
    {:graph g
     :vertex-explorer (G/bfs (G/get-vertex "Seattle" g) g)
     :curtain-progress 0
     :explore-iter-progress 0
     :stage :curtain}))

(defn translate
  [value start-min start-max end-min end-max]
  (+ (* (/ (- value start-min) (- start-max start-min))
        (- end-max end-min))
     end-min))


(def MIN-LON -125)
(def MAX-LON -68)
(def MIN-LAT 50)
(def MAX-LAT 25)
(defn point-to-coords
  [lat lon]
  [(translate lon MIN-LON MAX-LON 0 (q/width))
   (translate lat MIN-LAT MAX-LAT 0 (q/height))])

(defn draw-city!
  ([city] (draw-city! city (palette :light-gray) 1))
  ([city color stroke-weight]
   (let [[x y] (point-to-coords (:latitude city) (:longitude city))]
     (q/stroke color)
     (q/stroke-weight stroke-weight)
     ;;(q/fill (palette :bg))
     (q/fill (palette :bg-transparent))
     (q/ellipse x y 20 20))))


(defn draw-line-between-cities!
  [city1 city2]
  (let [[x1 y1]
        (point-to-coords (:latitude city1) (:longitude city1))
        [x2 y2]
        (point-to-coords (:latitude city2) (:longitude city2))]
    (q/stroke (palette :wispy-gray))
    (q/line x1 y1 x2 y2)))

(defn draw-cities!
  [graph]
  (doseq [v (map val (:vertices graph))]
    (doseq [u (map (fn [v] (G/get-vertex v graph)) (:connections v))]
      (draw-line-between-cities! (:value v) (:value u)))
    (draw-city! (:value v))))

(def next-draw-city (atom {}))

(defn cities-update
  [{:keys [graph vertex-explorer
           explore-iter-progress curtain-progress stage] :as state}]
  (case stage
    :explore
    (if (> (:explore-iter-progress state) 1)
      (do
        (go (let [v (<! vertex-explorer)]
              (swap! next-draw-city (fn [] (get v :value)))))
        (assoc state :explore-iter-progress 0))
      (assoc state
             :explore-iter-progress
             (+ explore-iter-progress explore-iter-inc)))
    :curtain
    (as-> state s
      ;; increase the curtain's gradient progress
      (assoc s :curtain-progress (+ curtain-progress curtain-inc))
      ;; move to the next stage if needed
      (if (> (:curtain-progress s) 1)
        (assoc s :stage (next-stage stage))
        s))))

(defn cities-draw
  [{:keys [stage graph] :as state}]
  (q/background (q/color 0 0 0 1))
  (draw-cities! graph)
  (case stage
    ;; Exploring along the BFS
    :explore
    (if (not (nil? @next-draw-city))
      (draw-city! @next-draw-city (palette :orange) 3))
    ;; Draw gradient curtain (unless it's already been lifted)
    :curtain
    (let [curtain (q/lerp-color
                   (palette :bg) (palette :bg-transparent)
                   (:curtain-progress state))]
      (println (:curtain-progress state))
      (q/stroke (q/color 0 0 0 1))
      (q/fill curtain)
      (q/rect 0 0 (q/width) (q/height)))))

(def CANVAS-WIDTHS (- (.-innerWidth js/window) 25))

(defn start [data]
  (log "startings...")
  (swap! initial-data (constantly data))
  (q/defsketch cities-graph-sketch
    :host "cities-graph-canvas"
    :size [CANVAS-WIDTHS 750]
    :setup cities-setup
    :update cities-update
    :draw cities-draw
    :middleware [m/fun-mode]))

;; Start the sketch once the JSON is loaded in
(go (start (<! (get-cities-data))))

