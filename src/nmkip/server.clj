(ns nmkip.server
  (:require [aleph.http :as http]
            [reitit.ring :as ring]
            [reitit.ring.middleware.parameters :as params]
            [clojure.string :as str]
            [clojure.core.async :as async]))

(def hashtags-ch (async/chan 1))

(defn handler [request]
  (let [hashtags (get-in request [:query-params "hashtags"]) 
        hashtags (some-> hashtags (#(str/split % #",")))]
    (when (not-empty hashtags)
      (async/go
        (async/>! hashtags-ch hashtags))))
  {:status 200 :body "ok"})

(def app
  (ring/ring-handler
   (ring/router
    [["/ping" (fn [_] {:status 200 :body "ok"})]
     ["/pong" {:post {:handler (fn [_] {:status 200 :body "ok"})}}]
     ["/tweets" {:post {:handler #'handler}}]]
    {:data {:middleware [params/parameters-middleware]}})
   (ring/create-default-handler)))

(defn start
  ([] (start 10000))
  ([port] (http/start-server #'app {:port port})))

(comment

  (app {:request-method :post
        :uri "/tweets"
        :query-params {:hashtags "clojure,clojurescript"}})

  (app {:request-method :get
        :uri "/ping"})

  (async/go
    (let [data (async/<! hashtags-ch)]
      (println data)))

  (async/go
    (let [timeout-chan (async/timeout 6000)]
      (-> 
       (async/alt!
         timeout-chan :timeout
         hashtags-ch ([hashtags] hashtags))
       println)))

  (def s (start))
  (.close s)
,)
