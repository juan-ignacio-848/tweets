(ns nmkip.tweets
  (:require [nmkip.twitter-wrapper :as twitter]
            [clojure.pprint :refer [pprint]]
            [clojure.core.async :refer [alt! go-loop timeout]]
            [nmkip.server :refer [start hashtags-ch]])
  (:gen-class))

(def server (start))

(defn- fetch-unseen-tweets! [terms]
  (try
    (twitter/unseen-tweets! terms)
    (catch Exception ex
      (println "Couldn't fetch tweets. " (.getMessage ex)))))

(defn- print-tweets [terms]
  (doseq [unseen (fetch-unseen-tweets! terms)]
    (pprint unseen)))

(defn -main [& args]
  (go-loop [x 20]
    (let [timeout-chan (timeout 20000)]
      (-> 
       (alt!
         timeout-chan []
         hashtags-ch ([hashtags] hashtags))
       print-tweets))
    (when (pos? x)
      (recur (dec x)))))

(comment
  (twitter/unseen-tweets! "clojure")
  (-main)

  ,)
