(ns nmkip.server
  (:require [aleph.http :as http]
            [reitit.ring :as ring]
            [reitit.ring.middleware.parameters :as params]
            [clojure.set :as set]
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

(comment
  (defn handler [_]
    {:status 200, :body "ok"})

  (defn wrap [handler id]
    (fn [request]
      (update (handler request) :via (fnil conj '()) id)))

  (defn wrap-enforce-roles [handler]
    (fn [{:keys [roles] :as request}]
      (let [required (some-> request (ring/get-match) :data :roles)]
        (if (and (seq required) (not (set/subset? required roles)))
          {:status 403, :body "forbidden"}
          (handler request)))))

  (def app
    (ring/ring-handler
     (ring/router
      ["/api" {:middleware [#(wrap % :api)]}
       ["/ping" handler]
       ["/admin" {:middleware [[wrap :admin]]
                  :roles #{:admin}}
        ["/ping" handler]
        ["/db" {:middleware [[wrap :db]]
                :delete {:middleware [[wrap :delete]]
                         :handler handler}}]]]
      {:data {:middleware [[wrap :top] [wrap-enforce-roles]]}}) ;; all routes
     (ring/create-default-handler)))

  (def app
    (ring/ring-handler
     (ring/router
      ["/api"
       ["/ping" handler]
       ["/admin" {:roles #{:admin}}
        ["/ping" handler]]]
      {:data {:middleware [wrap-enforce-roles]}})))

  (def s (http/start-server #'app {:port 10000}))
,)

(comment
  (app {:request-method :delete :uri "/api/admin/db"})
    
  (app {:request-method :get :uri "/api/admin/db"})
    
  (app {:request-method :get :uri "/api/ping"})
    
  (app {:request-method :get :uri "/api/admin/ping"})
  (app {:request-method :get :uri "/api/admin/ping" :roles #{:admin}})
    
  (.close s)
  ,)
