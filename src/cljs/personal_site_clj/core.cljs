(ns personal-site-clj.core
	(:require [quil.core :as q :include-macros true]
						[quil.middleware :as m]))

(enable-console-print!)

; Allow iteration over HTML elements
(extend-type js/HTMLCollection
	ISeqable
	(-seq [array] (array-seq array 0)))




; Binary (AVL) tree implementation
; thanks to https://two-wrongs.com/purely-functional-avl-trees-in-common-lisp.html


; a queue of state changes and affected nodes that the animation
; can pull from later
(def rotations (atom #queue []))

(defrecord AvlTree [key value left right])

(defn tree-height
	([tree] (tree-height tree 0))
	([tree height] 
		(if (nil? tree)
			height
			(max (tree-height (:left tree) (inc height))
					 (tree-height (:right tree) (inc height))))))

(defn balance-factor [node]
	"Get the balance factor of subtree rooted at node."
	(case (- (tree-height (:right node))
					 (tree-height (:left node)))
		-2 :imbalanced-left
		-1 :left-heavy
		 0 :balanced
		+1 :right-heavy
		+2 :imbalanced-right))

(declare avl-node)
(defn rotate-left [node]
	"Return tree rotated left."
	(let [{:keys [key value left right]} node
				height (tree-height node)]
		(swap! rotations conj {:type :rotate-left :key key})
		(avl-node
			(:key right)
			(:value right)
			(avl-node key value left (:left right))
			(:right right))))

(defn rotate-right [node]
	"Return tree rotated right."
	(let [{:keys [key value left right]} node
				height (tree-height node)]
		(swap! rotations conj {:type :rotate-right :key key})
		(avl-node
			(:key left)
			(:value left)
			(:left left)
			(avl-node key value (:right left) right))))

(defn avl-node [key value left right]
	(let [node (AvlTree. key value left right)]
		(case (balance-factor node)
			(:left-heavy :balanced :right-heavy)
				node

			:imbalanced-left
				(case (balance-factor left)
					:left-heavy
						(rotate-right node)
					:right-heavy
						(avl-node key value (rotate-left left) right))

			:imbalanced-right
				(case (balance-factor right)
					:left-heavy
						(avl-node key value left (rotate-right right))
					:right-heavy
						(rotate-left node)))))

(defn avl-insert [key value tree]
	(if (nil? tree)
		(avl-node key value nil nil)
		(avl-node
			(:key tree)
			(:value tree)
			(if (< key (:key tree))
				(avl-insert key value (:left tree))
				(:left tree))
			(if (< key (:key tree))
				(:right tree)
				(avl-insert key value (:right tree))))))

(defn lookup [key {:keys [node-key value left right] :as tree}]
	"Returns all values associated with key in tree."
	(cond
		(< key node-key) (lookup key left)
		(< node-key key) (lookup key right)
		:else (cons value
						(concat (lookup key left) (lookup key right)))))

(defn tree-tabs [n]
  (clojure.string/join(repeat n "\t")))

(defn tree-visualize
  ([tree] (tree-visualize tree 0))
	([{:keys [key value left right] :as tree} depth]
    (if tree
      (str (tree-visualize right (inc depth)) (tree-tabs depth) key "\n" (tree-visualize left (inc depth)))
      (str (tree-tabs depth) "~\n"))))

(defn tree-print [tree] (println (tree-visualize tree)))

(defn map->avl [key-val-map]
	(reduce
		(fn [tree [key val]] (avl-insert key val tree))
		nil
		(seq key-val-map)))


;	;	;	;	;	;	;	;	;	;	;	;	;
; Event handlers
;	;	;	;	;	;	;	;	;	;	;	;	;
(defn trigger-when-class-clicked [class-name f]
	(let [elements (.getElementsByClassName js/document class-name)]
		(doall (map #(.addEventListener % "click" f) elements))))

(defn toggle-menu [event]
	(let [hamburger (js/document.getElementById "hamburger")
				menu (js/document.getElementById "menu")]
		(.toggle (.-classList menu) "hidden")
		(.toggle (.-classList hamburger) "is-active")))
(defn close-menu [event]
	(let [hamburger (js/document.getElementById "hamburger")
				menu (js/document.getElementById "menu")]
		(.add (.-classList menu) "hidden")
		(.remove (.-classList hamburger) "is-active")))

(trigger-when-class-clicked "hamburger" toggle-menu)
(trigger-when-class-clicked "menu-link" close-menu)


;	;	;	;	;	;	;	;	;	;	;	;	;
; Animations & drawings
;	;	;	;	;	;	;	;	;	;	;	;	;

(def FRAME-RATE 20)
(def HALF-PI (/ Math/PI 2))
; flag to determine if things need to be redrawn in this frame
(def dirty-state (atom true))
;
(def old-positions (atom {}))

(def tree-settings
	{:x-spread 200
	 :node-r 20
	 :level-height 75})

(defn setup []
	(do
		(q/frame-rate FRAME-RATE)
		(q/background (q/color 0 0 0 1))
		(q/color-mode :hsb)
		(q/stroke 240)
		(q/stroke-weight 2)
		(let [tree (map->avl (into {} (map vector (range 0 1) (repeat 1 nil))))]
			(prn tree)
			(tree-visualize tree)
			; initial state
			{:x 400 :y 300 :tree tree :rotation-angle 0 :progress-to-next-step 0})))

(defn mouse-moved [state event]
	"Save angle between base of tree and mouse."
	(let [dy (- (:y state) (:y event))
				dx (- (:x state) (:x event))]
		(swap! dirty-state (constantly true))
		(assoc state :rotation-angle (+ (Math/atan2 dy dx) (/ Math/PI 2)))))

(defn update-tree [state]
	(let [update-time? (= 0 (mod (q/frame-count) (* 2 FRAME-RATE)))
				next-key (Math/ceil (/ (q/frame-count) (* 2 FRAME-RATE)))]
		(if (and update-time? (< (tree-height (:tree state)) 5))
			(do
				(swap! dirty-state (constantly true))
				(-> state
					(assoc :tree (avl-insert next-key nil (:tree state)))
					(assoc :progress-to-next-step 0)))
			(assoc state :progress-to-next-step (+ (:progress-to-next-step state) (/ 0.5 FRAME-RATE))))))

; (defn progress-to-next-step [] (/ (mod (/ FRAME-RATE 1) (q/frame-count)) FRAME-RATE))

(defn draw-tree
	([tree x y progress-to-next-step]
		(draw-tree tree x y nil nil 0 progress-to-next-step))
	([tree x y last-x last-y depth progress-to-next-step]
		(let [next-y (+ y (:level-height tree-settings))
					r (:node-r tree-settings)
					x-delta (/ (:x-spread tree-settings) (Math/pow 2 (inc depth)))
					last-position (get @old-positions (:key tree))]
			; draw line from parent
			(if (not (or (nil? last-x) (nil? last-y)))
				(do
					(q/line last-x (+ last-y (/ r 2)) x (- y (/ r 2)))))
			; apply any current rotations
			(q/push-matrix)
			(println last-position " at key " (:key tree))

			(let [old-x (get last-position :x)
						old-y (get last-position :y)
						p progress-to-next-step]
				(let [current-x (if (nil? old-x) x (+ (* p x) (* (- 1 p) old-x)))
							current-y (if (nil? old-y) y (+ (* p y) (* (- 1 p) old-y)))]
					(println "---------------" p)
					(println "old " old-x "," old-y " current " current-x "," current-y " latest " x "," y)
					; draw the ellipse
					(q/fill 100 98 60 0)
					(q/ellipse current-x current-y r r)
					(q/fill 240)
					(q/text (:key tree) (- current-x 5) (+ current-y 5))
					; and continue down the tree
					(if (nil? (:left tree))
						nil
						(draw-tree (:left tree) (- current-x x-delta) next-y current-x current-y (inc depth) p))
					(if (nil? (:right tree))
						nil
						(draw-tree (:right tree) (+ current-x x-delta) next-y current-x current-y (inc depth) p))
					(if (> progress-to-next-step 0.95) (swap! old-positions assoc (:key tree) {:x x :y y}))))

			

			; remove any rotations
			(q/pop-matrix))))



(defn draw [state]
	; traverse & draw the tree
	(when @dirty-state
		(println "re-drawing")
		(println @old-positions)
		(q/background (q/color 0 0 0 1))
		(q/translate (:x state) (:y state))
		(q/rotate (:rotation-angle state))
		(draw-tree (:tree state) 0 0 (:progress-to-next-step state))))
	; (swap! dirty-state (constantly false))
		

(q/defsketch fun-mode-times
	:host "test-canvas"
	:size [(- (.-innerWidth js/window) 25) 750]
	:setup setup
	:draw draw
	:update update-tree
	:mouse-moved mouse-moved
	:middleware [m/fun-mode])

(defn render [] nil)
