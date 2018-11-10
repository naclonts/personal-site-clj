(ns personal-site-clj.mail
	(:require [postal.core :as postal]
						[environ.core :refer [env]]))

(defn to-self [subject body]
	"Email self (based on environment variables)."
	(postal/send-message {:host	(env :email-host)
												:user	(env :email-host-user)
												:pass	(env :email-host-password)
												:ssl true}
											 {:from	(env :email-host-user)
												:to		(env :personal-email)
												:subject subject
												:body body}))

(defn contact-submission [{:keys [name email message] :as params}]
	"Send to self a message submitted in the contact form."
	(to-self
		; subject
		(str "New contact form submission from " name)
		; body
		(str
			"Contact Name:\n"
			name
			"\n\nEmail:\n"
			email
			"\n\nContent:\n"
			message
			"\n")))
