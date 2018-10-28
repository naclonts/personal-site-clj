(ns personal-site-clj.core
	(:require [quil.core :as q :include-macros true]))

(enable-console-print!)

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
