(ns nmkip.twitter-wrapper
    (:require [aleph.http :as http]
              [byte-streams :as bs]
              [cheshire.core :as json]
              [clojure.string :as str]
              [nmkip.cache :refer [cache-new!]]))

(def bearer-token (-> "Bearer "
                      (str (System/getenv "TWITTER_BEARER_TOKEN"))))

(def searching-terms (atom #{}))

(defn- ->model [data]
  (map (fn [t] {:created-at (:created_at t)
                :id (:id t)
                :text (:text t)})
       data))

(defn- fetch-request [terms {:keys [hashtag? fields]}]
  (let [terms (map (fn [term]
                     (if hashtag?
                       (if (str/starts-with? term "#") term (str "#" term))
                       term)) terms)
        query-terms (str/join " OR " terms)
        fields (str/join "," fields)]
    {:query-params {:tweet.fields fields
                    :query query-terms}
     :headers {"authorization" bearer-token}}))

(defn- fetch
  ([terms] (fetch terms nil))
  ([terms options]
   (let [terms (if (empty? terms) @searching-terms terms) 
         request (fetch-request terms options)]
     (if (empty? terms)
       []
       (-> @(http/get "https://api.twitter.com/2/tweets/search/recent" request)
           :body
           bs/to-string
           (json/decode keyword))))))

(defn- update-searching-terms! [terms override-terms]
  (if override-terms
     (reset! searching-terms (set terms))
     (swap! searching-terms into terms)))

(defn unseen-tweets!
  ([] (unseen-tweets! [] nil))
  ([terms] (unseen-tweets! terms nil))
  ([terms {:keys [override-terms] :or {override-terms false}}]
   (-> terms
       (update-searching-terms! override-terms)
       (fetch {:hashtag? true :fields ["created_at"]})
       :data
       ->model
       cache-new!)))

(comment

  (-> 
   @(http/get "https://api.twitter.com/2/tweets/search/recent?query=%23clojure&tweet.fields=entities"
              {:headers {"authorization" (str "Bearer " bearer-token)}})
   :body
   bs/to-string
   (json/decode keyword))

  (-> 
   @(http/get "https://api.twitter.com/2/tweets/search/recent?query=trump OR macri &tweet.fields=entities"
              {:headers {"authorization" (str "Bearer " bearer-token)}})
   :body
   bs/to-string
   (json/decode keyword))

  searching-terms
  (unseen-tweets! ["clojure" "covid" "trump"] {:override-terms true})

  (fetch ["clojure"] {:hashtag? true :fields ["entities"]})
  (fetch ["covid"] {:reset false :hashtag? true :fields ["created_at"]})
  (fetch ["colombia"] {:hashtag? true :fields ["created_at"]})
  ,)
