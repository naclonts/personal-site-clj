(ns personal-site-clj.core
	(:require [quil.core :as q :include-macros true]
						[quil.middleware :as m]))

(enable-console-print!)
(extend-type js/HTMLCollection
	ISeqable
	(-seq [array] (array-seq array 0)))


; Binary (AVL) tree implementation
; thanks to https://two-wrongs.com/purely-functional-avl-trees-in-common-lisp.html

(defrecord AvlTree [key value left right])

(defn tree-height
	([tree] (tree-height tree 0))
	([tree height] 
		(if (nil? tree)
			height
			(max (tree-height (.-left tree) (inc height))
					 (tree-height (.-right tree) (inc height))))))

(defn balance-factor [node]
	"Get the balance factor of subtree rooted at node."
	(case (- (tree-height (.-right node))
					 (tree-height (.-left node)))
		-2 :imbalanced-left
		-1 :left-heavy
		 0 :balanced
		+1 :right-heavy
		+2 :imbalanced-right))

(defn rotate-left [node]
	"Return tree rotated left."
	(let [{:keys [key value left right]} node
				height (tree-height node)]
		(avl-node
			(.-key right)
			(.-value right)
			(avl-node key value left (.-left right))
			(.-right right))))

(defn rotate-right [node]
	"Return tree rotated right."
	(let [{:keys [key value left right]} node
				height (tree-height node)]
		(avl-node
			(.-key left)
			(.-value left)
			(.-left left)
			(avl-node key value (.-right left) right))))

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
			(.-key tree)
			(.-value tree)
			(if (< key (.-key tree))
				(avl-insert key value (.-left tree))
				(.-left tree))
			(if (< key (.-key tree))
				(.-right tree)
				(avl-insert key value (.-right tree))))))

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
		(println (str "key: " key ", value: " value))
    (if tree
      (str (tree-visualize right (inc depth)) (tree-tabs depth) key "\n" (tree-visualize left (inc depth)))
      (str (tree-tabs depth) "~\n"))))

(defn tree-print [tree] (println (tree-visualize tree)))

(defn map->avl [key-val-map]
	(reduce
		(fn [tree [key val]] (avl-insert key val tree))
		nil
		(seq key-val-map)))




; Event handlers
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

; ; Animations & drawings

; (def tree-settings
; 	{:x-spread 200
; 	 :node-r 20
; 	 :level-height 50})

; (defn setup []
; 	(do
; 		(q/frame-rate 1)
; 		(q/background (q/color 0 0 0 1))
; 		(let [tree (seq->avl '(1 2 3 4 5 6 7 8 9))]
; 			(prn tree)
; 			(tree-visualise tree)
; 			; initial state
; 			{:x 400 :y 50 :tree tree})))

; (defn update-circle [state]
; 	(update-in state [:r] inc))

; (defn draw-tree
; 	([tree x y]
; 		(draw-tree tree x y 0))
; 	([tree x base-y depth]
; 		(let [y (+ base-y (:level-height tree-settings))
; 					r (:node-r tree-settings)
; 					x-delta (/ (:x-spread tree-settings) (Math/pow 2 (inc depth)))]
; 			(q/ellipse x y r r)
; 			(q/text (:el tree) (+ x 10) y)
; 			(if (nil? (:left tree))
; 				nil
; 				(draw-tree (:left tree) (- x x-delta) y (inc depth)))
; 			(if (nil? (:right tree))
; 				nil
; 				(draw-tree (:right tree) (+ x x-delta) y (inc depth))))))


; (defn draw [state]
; 	; traverse & draw the tree
; 	(draw-tree (:tree state) (:x state) (:y state)))

; ; (defn shrink [r]
; ; 	(max min-r (- r 3)))

; ; (defn mouse-moved [state event]
; ; 	(-> state
; ; 		(assoc :x (:x event) :y (:y event))
; ; 		(update-in [:r] shrink)))

; (q/defsketch fun-mode-times
; 	:host "test-canvas"
; 	:size [(- (.-innerWidth js/window) 25) 500]
; 	:setup setup
; 	:draw draw
; 	:update update-circle
; 	:middleware [m/fun-mode])

(defn render [] nil)
