(ns tweet-match-calc.core
  (:require [tweet-match-calc.db.mysql :as mysql]
            [tweet-match-calc.db.mongo :as mongo]
            [tweet-match-calc.calc.morpho :as morpho]
            [tweet-match-calc.calc.leven :as leven]
            [tweet-match-calc.api.twitter :as twitter]
            [clojure.string :as str])
  (:gen-class))


;; API利用用twitterアカウント
(def tw-accounts (read-string (slurp "config/twitter.clj")))

;; テストデータ
;; (def b-1 ["kanojo_hoshi_" "eclipse_rkt" "BaskeHi" "ilikesicp" "nobkz" "takuya199850" "mug_en" "charaoyukirin" "tetsuya1975m" "doumonnd"])
;; (def g-1 ["kareshi_hoshi_" "namida1055" "0428hrChi" "nemukyun1" "chomado" "Zwei_Megu" "xion_2574" "ManyaRiko" "gyuunyuu_umai" "tamakazura_yuri"])

;; １ユーザー毎の、相性ランキング、頻出ワードを取得する
(defn get-analyses [user candidates twitters sex]
  (loop [i 0
         analyses (atom [])
         user-data (morpho/get-tweet-analyze (Long/parseLong (:user_id user))
                                             (nth twitters (- (count twitters) 1)))]
    (when (< i (count candidates))
      (let [twitter (nth twitters (rem i (count twitters)))
            candidate (nth candidates i)]
        (Thread/sleep 30)
        (let [candidate-data (morpho/get-tweet-analyze (Long/parseLong (:user-id candidate))
                                                       twitter)]
          (swap! analyses conj (array-map :screen-name (:screen-name candidate)
                                          :profile-image (:profile-image candidate-data)
                                          :user-name (:user-name candidate-data)
                                          :leven (leven/levenshtein-distance (:text user-data)
                                                                             (:text candidate-data))))
          (if (= (+ i 1) (count candidates))
            (do
              (mongo/add-data (:user_id user)
                              (:screen_name user)
                              (:top-words user-data)
                              (sort-by :leven @analyses)
                              sex
                              (:profile-image user-data)
                              (:user-name user-data)
                              (:tweet user)
                              (:date user)
                              (:description user)
                              (:profile-back-url user-data))
              (mongo/add-couple-data (:user_id user)
                                     (:screen-name (nth (sort-by :leven @analyses) 0))
                                     (:leven (nth (sort-by :leven @analyses) 0))
                                     (:date user)
                                     )
              (mysql/complete-matching (:user_id user) sex)
              (println
               (array-map :screen-name (:screen_name user)
                          :user-name (:user-name user-data)
                          :prof-img (:profile-image user-data)
                          :top-words (:top-words user-data)
                          :ranking (sort-by :leven @analyses))))
            (recur (inc i) analyses user-data)))))))

;; バッチ処理用
(defn go-matching [users twitters sex]
  (if (= sex "b")
    (pmap #(get-analyses % (mongo/get-rnd-user "g") twitters sex) users)
    (pmap #(get-analyses % (mongo/get-rnd-user "b") twitters sex) users)))


;; 解析実行
(defn pararell-match []
  (while true
    (do
      (pvalues (go-matching (mysql/select-boys) tw-accounts "b")
               (go-matching (mysql/select-girls) tw-accounts "g"))
      (Thread/sleep 120000))))

;; main
(defn -main [& args]
    (pararell-match))


;; ランキングデータ初期化用
(defn fix-object [object]
  (let [id (:user_id object)
        name (:screen_name object)]
    (-> object
        (assoc :user-id id)
        (assoc :screen-name name))))

(defn init-ranking
  [num]
  (let [boys (mysql/get-rnd-boys num)
        girls (mysql/get-rnd-girls num)]
;    (mongo/all-clear "mach-ranking")
    (pvalues (pmap #(get-analyses % (map fix-object girls) tw-accounts "b") boys)
             (pmap #(get-analyses % (map fix-object boys)  tw-accounts "g") girls))))
