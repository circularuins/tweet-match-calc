(ns tweet-match-calc.db.mysql
  (:require [clj-time.core :as t]
            [clj-time.local :as tl]
            [clj-time.format :as tf]
            [clojure.string :as str]))

(def custom-formatter (tf/formatter "yyyy/MM/dd HH:mm:ss"))
(def minutes-ago (t/minus (tl/local-now) (t/minutes 1)))

(use 'korma.db)

(defdb kanojodb
  (mysql
   {:db "twitter_bot"
    :port "3306"
    :user "wake"
    :password "wakenatsuhiko"}))

(use 'korma.core)

(defentity kanojo_hoshi)

(defn select-by-name [name]
  (select kanojo_hoshi
          (fields :name :screen_name :user_id :date)
          (where {:name name})
          (order :date :DESC)))

(defn select-from-now []
  (select kanojo_hoshi
          (fields :name :screen_name :user_id :date)
          (where (> :date (.toString minutes-ago)))
          (order :date :DESC)
          (limit 3)))
