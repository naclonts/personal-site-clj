(ns personal-site-clj.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [personal-site-clj.core-test]
   [personal-site-clj.common-test]))

(enable-console-print!)

(doo-tests 'personal-site-clj.core-test
           'personal-site-clj.common-test)
