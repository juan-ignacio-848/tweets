(ns nmkip.cache
  (:require [clojure.core.cache.wrapped :as cache]))

#_(def tweets-cache (cache/ttl-cache-factory {} :ttl (* 1000 60 5)))
(def tweets-cache (cache/fifo-cache-factory {}))

(defn cache-new! [tweets]
  (reduce (fn [unseen tweet]
            (if (cache/has? tweets-cache (:id tweet))
              unseen
              (do
                (cache/through-cache tweets-cache (:id tweet) (constantly tweet))
                (conj unseen tweet))))
          [] tweets))

(comment


  (def C1 (cache/fifo-cache-factory {:a 1, :b 2}))

  (def C1' (if (cache/has? C1 :c)
             (cache/hit C1 :c)
             (cache/miss C1 :c 42)))

  (cache/lookup C1 :b)
  (get C1 :b)

  (def slow-db {:c 2})
  (cache/through-cache C1 :c)
  (cache/through-cache C1 :c (fn [k] (k slow-db)))

  (cache/hit C1 :c)

  (cache/through-cache tweets-cache 23 (constantly {:a 2 :id 23 :b 33}))
  (cache/lookup tweets-cache 23)
  ,)
