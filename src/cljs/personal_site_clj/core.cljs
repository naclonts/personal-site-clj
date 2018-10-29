(ns personal-site-clj.core
	(:require [quil.core :as q :include-macros true]))

(enable-console-print!)

(extend-type js/HTMLCollection
	ISeqable
	(-seq [array] (array-seq array 0)))

; Event handlers
(defn trigger-when-clicked [class-name f]
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

(trigger-when-clicked "hamburger" toggle-menu)
(trigger-when-clicked "menu-link" close-menu)

; Animations & drawings
(defn setup []
	(q/frame-rate 1)
	(q/background (q/color 0 0 0 1)))

(defn draw []
	(q/stroke (q/random 255))
	(q/stroke-weight (q/random 10))
	(q/fill (q/random 255))
	
	(let [diam (q/random 100)
				x		 (q/random (q/width))
				y		 (q/random (q/height))]
		(q/ellipse x y diam diam)))

(q/defsketch personal-site-clj
	:host "test-canvas"
	:settings #(q/smooth 2)
	:setup setup
	:draw draw
	:size [(- (.-innerWidth js/window) 25) 300])




(defn render [] nil)
; (defn render []
; 	(set! (.-innerHTML (js/document.getElementById "replace-this"))
; 				"TEXT HERE"))
