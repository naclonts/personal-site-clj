(ns personal-site-clj.graph)

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

