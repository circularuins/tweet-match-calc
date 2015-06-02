(ns tweet-match-calc.db.mongo
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :as mq]
            [monger.operators :as mo]
            [clojure.string :as str]
            [clj-time
             [core :as t]
             [local :as tl]
             [coerce :as coerce]])
  (:import java.util.Date
           java.util.regex.Pattern
           org.bson.types.ObjectId))

(defn fix-object [object]
  (let [id (:_id object)]
    (-> object
        (assoc :id (str id))
        (dissoc :_id))))

(def db (mg/get-db (mg/connect) "matching-db"))

(defn add-data
  [screen-name top-words ranking]
  (let [coll "mach-ranking"]
    (mc/update db coll {:user-id id}
               {:screen-name screen-name
                :top-words top-words
                :ranking ranking
                :date (.toString (tl/local-now))}
               {:upsert true})))

(defn get-data
  [screen-name]
  (->> (mq/with-collection db "user-data"
         (mq/find {:chat-room room})
         (mq/sort (array-map :date 1)))
       (map fix-object)))
