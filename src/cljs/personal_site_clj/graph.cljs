(ns personal-site-clj.graph
  (:require [cljs.core.async
             :as async
             :refer [>! <! go chan close!]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph structure implementation
(defrecord Graph [vertices])
(defrecord Vertex
    ;; Vertex structure:
		;; 	- key : unique string
		;; 	- value : payload
		;; 	- connections : set of keys to connected vertices
		;; 	- state : search state (undiscovered, discovered,
    ;;            or completely-explored)
		;; 	- parent : key of parent vertex
    [key value connections state parent])

(defn add-vertex
  "Add a new vertex with the given key/value to a graph."
  [key value {:keys [vertices] :as graph}]
  (->Graph (assoc
            vertices
            key
            (->Vertex key value #{} :undiscovered nil))))

(defn get-vertex [key {:keys [vertices] :as graph}]
  (get vertices key))

(defn connected? [f t]
  (contains? (:connections f) (:key t)))

(defn add-connection
  "Returns vertex f, now connected to vertex t."
  [f t]
  (assoc f :connections (conj (:connections f) (:key t))))

(defn add-edge
  "Create an undirected edge between vertices f and t."
  [f t {:keys [vertices] :as graph}]
  (->Graph
   (-> vertices
       (assoc (:key t) (add-connection t f))
       (assoc (:key f) (add-connection f t)))))

(defn print-vertex [v]
  (println (str
              (:key v)
              "\t\t- conn: " (:connections v)
              "\t\t- state: " (:state v)
              "\t\t- parent: " (:parent v))))

(defn print-graph [{:keys [vertices] :as g}]
  (println "Graph:")
  (doseq [v (map val vertices)]
    (print-vertex v)))

(defn traverse [{:keys [vertices] :as g}]
  "Returns a channel that receives each vertex of `g`."
  (let [out (chan)]
    (doseq [v (map val vertices)]
      (go (>! out v)))
    (async/take (count vertices) out)))

;; (defn bfs2
;;   "Returns a channel that emits vertices in the order they are
;;   discovered in a Breadth First Search."
;;   [start graph]
;;   (let [out (chan)]
;;     (loop [vertices {}
;;            explored #{start}
;;            frontier #queue [start]]
;;       (if (empty? frontier)
;;         ;; Once we're done, return the updated graph and channel
;;         [(assoc graph :vertices vertices)
;;          out]
;;         ;; Continue
;;         (let [v (peek frontier)
;;               neighbors (map
;;                          (fn [key]
;;                            (assoc (get-vertex key graph)
;;                                   :parent (:key v)))
;;                          (:connections v))]
;;           (go (>! out v))
;;           (recur
;;            (assoc vertices (:key v) v)
;;            (into explored neighbors)
;;            (into (pop frontier) (remove explored neighbors))))))))

(defn bfs
  [start graph]
  (let [out (chan)]
    (loop [discovered #queue [start]
           discovered-map {(:key start) true}
           u start]
      (if (nil? u)
        [graph out]
        (let [neighbor-keys
              (filter #(not (contains? discovered-map %))
                      (:connections u))
              neighbors
              (map #(assoc (get-vertex % graph) :parent (:key u))
                   neighbor-keys)
              new-discovered
              (as-> discovered ds
                (into #queue [] (concat ds neighbors)))]
          (go (>! out u))
          (recur
           ;; Record the neighbors as "discovered" and pop the first
           (pop new-discovered)
           ;; Record the discovered neighbors in a hash map, as well,
           ;; for fast lookup
           (into discovered-map
                 (map #(hash-map % true) neighbor-keys))
           ;; And move on to exploring the first discovered
           (peek new-discovered)))))))

