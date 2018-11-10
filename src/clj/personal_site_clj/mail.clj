(ns personal-site-clj.mail
	(:require [postal.core :as postal]
						[environ.core :refer [env]]))

(defn mail []
	(postal/send-message {:host	(env :email-host)
												:user	(env :email-host-user)
												:pass	(env :email-host-password)
												:ssl true}
											 {:from	(env :email-host-user)
												:to		(env :personal-email)
												:subject "Hi this is a test from clojure!"
												:body "Hope this works"}))
