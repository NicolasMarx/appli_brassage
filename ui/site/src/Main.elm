module Main exposing (main)

import Browser
import Char
import HopsV2 exposing (Hop, decoder, formatMl, formatPct)
import Html exposing (Html, button, div, h1, h2, input, label, option, p, span, text)
import Html.Attributes as A
import Html.Events as E
import Http
import Json.Decode as D
import Set
import String


-- MODEL


type alias Filters =
    { q : String
    , aromas : List String
    , usage : List String
    , beers : List String
    , alphaContains : Float
    }


type alias Pending =
    { q : String
    , aromas : String
    , usage : String
    , beers : String
    , alphaContains : Float
    }


type alias Suggestions =
    { aromas : List String
    , usage : List String
    , beers : List String
    , countries : List String
    }


type alias Model =
    { loading : Bool
    , error : Maybe String
    , hops : List Hop
    , pending : Pending
    , applied : Filters
    , sugg : Suggestions
    , modal : Maybe Hop
    }


emptyPending : Pending
emptyPending =
    { q = "", aromas = "", usage = "", beers = "", alphaContains = 0 }


emptyFilters : Filters
emptyFilters =
    { q = "", aromas = [], usage = [], beers = [], alphaContains = 0 }


emptySuggestions : Suggestions
emptySuggestions =
    { aromas = [], usage = [], beers = [], countries = [] }


init : () -> ( Model, Cmd Msg )
init _ =
    ( { loading = True
      , error = Nothing
      , hops = []
      , pending = emptyPending
      , applied = emptyFilters
      , sugg = emptySuggestions
      , modal = Nothing
      }
    , getHops
    )



-- UPDATE


type Msg
    = GotHops (Result Http.Error (List Hop))
    | SetQ String
    | SetAromas String
    | SetUsage String
    | SetBeers String
    | SetAlphaContains String
    | CommitAromas
    | CommitUsage
    | CommitBeers
    | AddAromaTag String
    | RemoveAromaTag String
    | AddUsageTag String
    | RemoveUsageTag String
    | AddBeerTag String
    | RemoveBeerTag String
    | Open Hop
    | CloseModal
    | NoOp


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        GotHops (Ok hs) ->
            let
                aromasS =
                    hs |> List.concatMap .aromas |> normStrings |> uniqSorted

                usageS =
                    hs |> List.filterMap .hopType |> normStrings |> uniqSorted

                beersS =
                    hs |> List.concatMap .beerStyles |> normStrings |> uniqSorted

                countriesS =
                    hs |> List.filterMap .country |> List.map String.toUpper |> uniqSorted
            in
            ( { model
                | loading = False
                , hops = hs
                , sugg = { aromas = aromasS, usage = usageS, beers = beersS, countries = countriesS }
              }
            , Cmd.none
            )

        GotHops (Err e) ->
            ( { model | loading = False, error = Just (httpErrorToString e) }, Cmd.none )

        SetQ s ->
            updatePending (setPendingQ s) model

        SetAromas s ->
            updatePending (setPendingAromas s) model

        SetUsage s ->
            updatePending (setPendingUsage s) model

        SetBeers s ->
            updatePending (setPendingBeers s) model

        SetAlphaContains s ->
            case String.toFloat s of
                Just v ->
                    updatePending (setPendingAlpha (clamp 0 50 v)) model

                Nothing ->
                    ( model, Cmd.none )

        -- ENTER valide → ajoute des tags + vide le champ
        CommitAromas ->
            let
                toks = splitWords model.pending.aromas
                applied1 = withAromas (addMany toks model.applied.aromas) model.applied
                pending1 = clearPendingAromas model.pending
            in
            ( { model | applied = applied1, pending = pending1 }, Cmd.none )

        CommitUsage ->
            let
                toks = splitWords model.pending.usage
                applied1 = withUsage (addMany toks model.applied.usage) model.applied
                pending1 = clearPendingUsage model.pending
            in
            ( { model | applied = applied1, pending = pending1 }, Cmd.none )

        CommitBeers ->
            let
                toks = splitWords model.pending.beers
                applied1 = withBeers (addMany toks model.applied.beers) model.applied
                pending1 = clearPendingBeers model.pending
            in
            ( { model | applied = applied1, pending = pending1 }, Cmd.none )

        AddAromaTag raw ->
            let
                v = normalize raw
            in
            ( { model | applied = withAromas (addOne v model.applied.aromas) model.applied }, Cmd.none )

        RemoveAromaTag raw ->
            let
                v = normalize raw
            in
            ( { model | applied = withAromas (removeOne v model.applied.aromas) model.applied }, Cmd.none )

        AddUsageTag raw ->
            let
                v = normalize raw
            in
            ( { model | applied = withUsage (addOne v model.applied.usage) model.applied }, Cmd.none )

        RemoveUsageTag raw ->
            let
                v = normalize raw
            in
            ( { model | applied = withUsage (removeOne v model.applied.usage) model.applied }, Cmd.none )

        AddBeerTag raw ->
            let
                v = normalize raw
            in
            ( { model | applied = withBeers (addOne v model.applied.beers) model.applied }, Cmd.none )

        RemoveBeerTag raw ->
            let
                v = normalize raw
            in
            ( { model | applied = withBeers (removeOne v model.applied.beers) model.applied }, Cmd.none )

        Open h ->
            ( { model | modal = Just h }, Cmd.none )

        CloseModal ->
            ( { model | modal = Nothing }, Cmd.none )

        NoOp ->
            ( model, Cmd.none )



-- SMALL HELPERS
-- (A) Helpers "update" pour Pending / Filters sans syntaxe { r | ... }


setPendingQ : String -> Pending -> Pending
setPendingQ s p =
    { q = s, aromas = p.aromas, usage = p.usage, beers = p.beers, alphaContains = p.alphaContains }


setPendingAromas : String -> Pending -> Pending
setPendingAromas s p =
    { q = p.q, aromas = s, usage = p.usage, beers = p.beers, alphaContains = p.alphaContains }


setPendingUsage : String -> Pending -> Pending
setPendingUsage s p =
    { q = p.q, aromas = p.aromas, usage = s, beers = p.beers, alphaContains = p.alphaContains }


setPendingBeers : String -> Pending -> Pending
setPendingBeers s p =
    { q = p.q, aromas = p.aromas, usage = p.usage, beers = s, alphaContains = p.alphaContains }


setPendingAlpha : Float -> Pending -> Pending
setPendingAlpha v p =
    { q = p.q, aromas = p.aromas, usage = p.usage, beers = p.beers, alphaContains = v }


clearPendingAromas : Pending -> Pending
clearPendingAromas p =
    { q = p.q, aromas = "", usage = p.usage, beers = p.beers, alphaContains = p.alphaContains }


clearPendingUsage : Pending -> Pending
clearPendingUsage p =
    { q = p.q, aromas = p.aromas, usage = "", beers = p.beers, alphaContains = p.alphaContains }


clearPendingBeers : Pending -> Pending
clearPendingBeers p =
    { q = p.q, aromas = p.aromas, usage = p.usage, beers = "", alphaContains = p.alphaContains }


withAromas : List String -> Filters -> Filters
withAromas xs a =
    { q = a.q, aromas = xs, usage = a.usage, beers = a.beers, alphaContains = a.alphaContains }


withUsage : List String -> Filters -> Filters
withUsage xs a =
    { q = a.q, aromas = a.aromas, usage = xs, beers = a.beers, alphaContains = a.alphaContains }


withBeers : List String -> Filters -> Filters
withBeers xs a =
    { q = a.q, aromas = a.aromas, usage = a.usage, beers = xs, alphaContains = a.alphaContains }


applyFromPending : Pending -> Filters
applyFromPending p =
    { q = p.q
    , aromas = splitWords p.aromas
    , usage = splitWords p.usage
    , beers = splitWords p.beers
    , alphaContains = p.alphaContains
    }


-- (B) update pending + applied ensemble


updatePending : (Pending -> Pending) -> Model -> ( Model, Cmd Msg )
updatePending f model =
    let
        p1 = f model.pending
    in
    ( { model | pending = p1, applied = applyFromPending p1 }, Cmd.none )



-- GENERIC HELPERS


clamp : Float -> Float -> Float -> Float
clamp lo hi x =
    if x < lo then
        lo
    else if x > hi then
        hi
    else
        x


splitWords : String -> List String
splitWords s =
    s
        |> String.split ","
        |> List.concatMap (String.split " ")
        |> List.map String.trim
        |> List.filter (\x -> x /= "")
        |> List.map normalize


normalize : String -> String
normalize =
    String.toLower << String.trim


addOne : String -> List String -> List String
addOne v xs =
    if List.any (\x -> x == v) xs then
        xs
    else
        xs ++ [ v ]


addMany : List String -> List String -> List String
addMany vs xs =
    List.foldl addOne xs vs


removeOne : String -> List String -> List String
removeOne v xs =
    List.filter (\x -> x /= v) xs


normStrings : List String -> List String
normStrings xs =
    xs
        |> List.map String.trim
        |> List.filter (\x -> x /= "")
        |> List.map (\x -> String.toLower x |> titleCase)


uniqSorted : List String -> List String
uniqSorted xs =
    xs |> Set.fromList |> Set.toList |> List.sort


titleCase : String -> String
titleCase s =
    let
        lower = String.toLower s
    in
    case String.uncons lower of
        Nothing ->
            ""

        Just ( c, rest ) ->
            String.fromChar (Char.toUpper c) ++ rest


onEnter : msg -> Html.Attribute msg
onEnter msg =
    E.on "keydown"
        (D.field "key" D.string
            |> D.andThen (\k -> if k == "Enter" then D.succeed msg else D.fail "nope")
        )


onClickStop : msg -> Html.Attribute msg
onClickStop msg =
    E.stopPropagationOn "click" (D.succeed ( msg, True ))



-- VIEW


main : Program () Model Msg
main =
    Browser.element
        { init = init
        , view = view
        , update = update
        , subscriptions = \_ -> Sub.none
        }


view : Model -> Html Msg
view model =
    div [ A.class "page" ]
        [ div [ A.class "page__bg" ] []
        , div [ A.class "container" ]
            [ h1 [ A.class "page-title" ] [ text "Houblons" ]
            , div [ A.class "layout" ]
                [ div [ A.class "sidebar" ]
                    [ controls model
                    , filtersPanel model.applied
                    ]
                , grid model (applyFilters model.hops model.applied)
                ]
            , modalView model.modal
            ]
        ]


controls : Model -> Html Msg
controls model =
    let
        alphaLabel =
            "Afficher si α contient " ++ String.fromFloat model.pending.alphaContains ++ " %"
    in
    -- Empilement vertical (géré côté CSS .layout .sidebar .controls)
    div [ A.class "controls" ]
        [ -- Recherche (placeholder = titre, pas de label externe)
          div [ A.class "field" ]
            [ input
                [ A.type_ "search"
                , A.placeholder "Recherche (nom / origine)"
                , A.class "input"
                , A.value model.pending.q
                , E.onInput SetQ
                ]
                []
            ]
        , -- Arômes
          div [ A.class "field" ]
            [ input
                [ A.type_ "text"
                , A.placeholder "Arômes"
                , A.class "input"
                , A.value model.pending.aromas
                , A.attribute "list" "aromas"
                , E.onInput SetAromas
                , onEnter CommitAromas
                ]
                []
            , Html.node "datalist" [ A.id "aromas" ] (List.map (\a -> option [] [ text a ]) model.sugg.aromas)
            ]
        , -- Usage du houblon
          div [ A.class "field" ]
            [ input
                [ A.type_ "text"
                , A.placeholder "Usage du houblon"
                , A.class "input"
                , A.value model.pending.usage
                , A.attribute "list" "usage"
                , E.onInput SetUsage
                , onEnter CommitUsage
                ]
                []
            , Html.node "datalist" [ A.id "usage" ] (List.map (\u -> option [] [ text u ]) model.sugg.usage)
            ]
        , -- Types de bières
          div [ A.class "field" ]
            [ input
                [ A.type_ "text"
                , A.placeholder "Type de bières"
                , A.class "input"
                , A.value model.pending.beers
                , A.attribute "list" "beers"
                , E.onInput SetBeers
                , onEnter CommitBeers
                ]
                []
            , Html.node "datalist" [ A.id "beers" ] (List.map (\b -> option [] [ text b ]) model.sugg.beers)
            ]
        , -- Slider alpha (on garde l’étiquette pour la valeur dynamique)
          div [ A.class "field" ]
            [ label [] [ text alphaLabel ]
            , input
                [ A.type_ "range", A.min "0", A.max "50", A.step "0.5"
                , A.value (String.fromFloat model.pending.alphaContains)
                , E.onInput SetAlphaContains
                ]
                []
            ]
        ]


filtersPanel : Filters -> Html Msg
filtersPanel f =
    let
        row title_ chips =
            if List.isEmpty chips then
                text ""
            else
                div [ A.class "filters-panel__row" ]
                    [ span [ A.class "filters-panel__label" ] [ text title_ ]
                    , div
                        [ A.style "display" "flex"
                        , A.style "flex-wrap" "wrap"
                        , A.style "gap" "8px"
                        ]
                        chips
                    ]

        aromaChips =
            f.aromas
                |> List.map (\a -> removableChip "chip chip--aroma" (titleCase a) (RemoveAromaTag a))

        usageChips =
            f.usage
                |> List.map (\u -> removableChip "chip chip--type" (titleCase u) (RemoveUsageTag u))

        beerChips =
            f.beers
                |> List.map (\b -> removableChip "chip chip--beer" (titleCase b) (RemoveBeerTag b))

        hasSomething =
            (not (List.isEmpty f.aromas))
                || (not (List.isEmpty f.usage))
                || (not (List.isEmpty f.beers))
    in
    if not hasSomething then
        text ""
    else
        div
            [ A.class "filters-panel"
            ]
            [ h2 [ A.class "filters-panel__title" ] [ text "Filtres" ]
            , row "Arômes" aromaChips
            , row "Usages" usageChips
            , row "Styles" beerChips
            ]


removableChip : String -> String -> Msg -> Html Msg
removableChip classes label_ removeMsg =
    span
        [ A.class classes
        , A.style "display" "inline-flex"
        , A.style "align-items" "center"
        , A.style "gap" "6px"
        ]
        [ text label_
        , span
            [ A.class "chip__close"
            , onClickStop removeMsg
            , A.attribute "role" "button"
            , A.attribute "aria-label" ("Supprimer " ++ label_)
            ]
            [ text "×" ]
        ]


field : String -> List (Html Msg) -> Html Msg
field lbl children =
    div [ A.class "field" ]
        [ label [] [ text lbl ]
        , div [] children
        ]


grid : Model -> List Hop -> Html Msg
grid model hops_ =
    if model.loading then
        div [ A.class "status" ] [ text "Chargement…" ]

    else
        case model.error of
            Just e ->
                div [ A.class "error" ] [ text e ]

            Nothing ->
                if List.isEmpty hops_ then
                    div [ A.class "status" ] [ text "Aucun résultat" ]
                else
                    div [ A.class "grid" ] (List.map card hops_)


card : Hop -> Html Msg
card h =
    div [ A.class "card", E.onClick (Open h) ]
        [ -- titre + puces à droite
          div
            [ A.class "card__title-row"
            , A.style "display" "flex"
            , A.style "align-items" "center"
            , A.style "justify-content" "space-between"
            , A.style "gap" "12px"
            ]
            [ h2
                [ A.class "card__title"
                , A.style "margin" "0"
                , A.style "flex" "1 1 auto"
                ]
                [ text h.name ]
            , div
                [ A.class "title-chips"
                , A.style "display" "flex"
                , A.style "gap" "8px"
                , A.style "flex-shrink" "0"
                ]
                [ span
                    [ A.class "chip chip--type"
                    , onClickStop
                        (case h.hopType of
                            Just u ->
                                AddUsageTag u

                            Nothing ->
                                NoOp
                        )
                    ]
                    [ text (Maybe.withDefault "—" (Maybe.map titleCase h.hopType)) ]
                , span [ A.class "chip chip--origin" ] [ text (Maybe.withDefault "—" (Maybe.map String.toUpper h.country)) ]
                ]
            ]
        , div [ A.class "card__body" ]
            [ kv "α min" (formatPct h.alphaMin)
            , kv "α max" (formatPct h.alphaMax)
            , kv "β min" (formatPct h.betaMin)
            , kv "β max" (formatPct h.betaMax)
            , kv "Huiles min" (formatMl h.oilsMin)
            , kv "Huiles max" (formatMl h.oilsMax)
            ]
        , div
            [ A.class "card__footer note"
            , A.style "display" "flex"
            , A.style "flex-direction" "column"
            , A.style "align-items" "flex-start"
            , A.style "gap" "8px"
            ]
            [ tags "aroma" h.aromas "chip chip--aroma"
            , tags "beer"  h.beerStyles "chip chip--beer"
            ]
        ]


kv : String -> String -> Html Msg
kv k v =
    div [ A.class "kv" ]
        [ span [ A.class "kv__k" ] [ text k ]
        , span [ A.class "kv__v" ] [ text v ]
        ]


-- ✅ CORRECTIF: tags prend 3 arguments (group xs baseClass)
tags : String -> List String -> String -> Html Msg
tags group xs baseClass =
    let
        render s =
            let
                addMsg =
                    case group of
                        "aroma" ->
                            AddAromaTag s

                        "beer" ->
                            AddBeerTag s

                        _ ->
                            NoOp
            in
            span [ A.class baseClass, onClickStop addMsg ] [ text s ]
    in
    div
        [ A.class ("tag-group group--" ++ group)
        , A.style "display" "flex"
        , A.style "flex-wrap" "wrap"
        , A.style "gap" "6px"
        , A.style "align-items" "center"
        , A.style "justify-content" "flex-start"
        , A.style "width" "100%"
        ]
        (List.map render xs)



-- MODAL


modalView : Maybe Hop -> Html Msg
modalView maybeHop =
    case maybeHop of
        Nothing ->
            text ""

        Just h ->
            div [ A.class "modal__backdrop", E.onClick CloseModal ]
                [ div [ A.class "modal", E.stopPropagationOn "click" (D.succeed ( NoOp, True )) ]
                    [ div [ A.class "modal__head" ]
                        [ h2 [ A.class "modal__title" ] [ text h.name ]
                        , p [ A.class "modal__desc" ] [ text (Maybe.withDefault "" h.description) ]
                        ]
                    , div [ A.class "modal__content" ]
                        [ div [ A.class "modal__grid" ]
                            [ sectionOil "Huiles" h
                            , sectionMeta "Caractéristiques" h
                            ]
                        ]
                    , div [ A.class "modal__actions" ]
                        [ button [ A.class "btn btn--ghost", E.onClick CloseModal ] [ text "Fermer" ] ]
                    ]
                ]


sectionMeta : String -> Hop -> Html Msg
sectionMeta title h =
    div [ A.class "section" ]
        [ h2 [ A.class "section__title" ] [ text title ]
        , div [ A.class "section__grid" ]
            [ kv "Origine" (Maybe.withDefault "—" (Maybe.map String.toUpper h.country))
            , kv "Usage" (Maybe.withDefault "—" (Maybe.map titleCase h.hopType))
            , kv "Alpha" (rangePct h.alphaMin h.alphaMax)
            , kv "Bêta" (rangePct h.betaMin h.betaMax)
            , kv "Arômes" (if List.isEmpty h.aromas then "—" else String.join ", " h.aromas)
            , kv "Styles" (if List.isEmpty h.beerStyles then "—" else String.join ", " h.beerStyles)
            ]
        ]


sectionOil : String -> Hop -> Html Msg
sectionOil title h =
    let
        bar label lo hi suf =
            oilbar label (Maybe.map (\x -> x * 100) lo) (Maybe.map (\x -> x * 100) hi) suf
    in
    div [ A.class "section" ]
        [ h2 [ A.class "section__title" ] [ text title ]
        , div []
            [ bar "Myrcène" h.myrceneMin h.myrceneMax "%"
            , bar "Humulène" h.humuleneMin h.humuleneMax "%"
            , bar "Caryophyllène" h.caryophylleneMin h.caryophylleneMax "%"
            , bar "Farnésène" h.farneseneMin h.farneseneMax "%"
            , kv "Total huiles" (rangeMl h.oilsMin h.oilsMax)
            ]
        ]


rangePct : Maybe Float -> Maybe Float -> String
rangePct lo hi =
    case ( lo, hi ) of
        ( Just a, Just b ) ->
            formatPct (Just a) ++ " – " ++ formatPct (Just b)

        ( Just a, Nothing ) ->
            formatPct (Just a)

        ( Nothing, Just b ) ->
            formatPct (Just b)

        _ ->
            "–"


rangeMl : Maybe Float -> Maybe Float -> String
rangeMl lo hi =
    case ( lo, hi ) of
        ( Just a, Just b ) ->
            formatMl (Just a) ++ " – " ++ formatMl (Just b)

        ( Just a, Nothing ) ->
            formatMl (Just a)

        ( Nothing, Just b ) ->
            formatMl (Just b)

        _ ->
            "–"



-- FILTERS


applyFilters : List Hop -> Filters -> List Hop
applyFilters hops f =
    let
        tokens =
            f.q
                |> String.toLower
                |> String.split ","
                |> List.map String.trim
                |> List.filter (\t -> t /= "")

        matchQ h =
            if List.isEmpty tokens then
                True
            else
                let
                    subject =
                        String.toLower h.name
                            ++ " "
                            ++ String.toLower (Maybe.withDefault "" h.country)
                            ++ " "
                            ++ String.toLower (String.join " " h.aromas)
                            ++ " "
                            ++ String.toLower (String.join " " h.beerStyles)
                in
                List.all (\t -> String.contains t subject) tokens

        matchList want have =
            List.isEmpty want
                || List.all (\w -> List.any (\hv -> String.toLower hv |> String.contains w) have) want

        matchUsage h =
            case h.hopType of
                Nothing ->
                    List.isEmpty f.usage

                Just u ->
                    matchList f.usage [ u ]

        matchBeers h =
            matchList f.beers h.beerStyles

        matchAromas h =
            matchList f.aromas h.aromas

        matchAlpha h =
            let
                v = f.alphaContains
                minPct = Maybe.map ((*) 100) h.alphaMin
                maxPct = Maybe.map ((*) 100) h.alphaMax
            in
            if v <= 0 then
                True
            else
                case ( minPct, maxPct ) of
                    ( Just lo, Just hi ) ->
                        lo <= v && v <= hi

                    ( Just lo, Nothing ) ->
                        lo <= v

                    ( Nothing, Just hi ) ->
                        v <= hi

                    _ ->
                        False
    in
    hops |> List.filter (\h -> matchQ h && matchAromas h && matchUsage h && matchBeers h && matchAlpha h)



-- OIL BAR


oilbar : String -> Maybe Float -> Maybe Float -> String -> Html Msg
oilbar label lo hi suf =
    let
        ( left_, width_ ) =
            case ( lo, hi ) of
                ( Just a, Just b ) ->
                    ( String.fromFloat a, String.fromFloat (max 0 (b - a)) )

                ( Just a, Nothing ) ->
                    ( String.fromFloat a, "0" )

                ( Nothing, Just b ) ->
                    ( "0", String.fromFloat b )

                _ ->
                    ( "0", "0" )

        valueText =
            case ( lo, hi ) of
                ( Just a, Just b ) ->
                    String.fromFloat a ++ "–" ++ String.fromFloat b ++ suf

                ( Just a, Nothing ) ->
                    String.fromFloat a ++ suf

                ( Nothing, Just b ) ->
                    String.fromFloat b ++ suf

                _ ->
                    "–"
    in
    div [ A.class "oilbar" ]
        [ span [ A.class "oilbar__label" ] [ text label ]
        , div [ A.class "oilbar__track" ]
            [ div
                [ A.class "oilbar__range"
                , A.style "left" (left_ ++ "%")
                , A.style "width" (width_ ++ "%")
                ]
                []
            ]
        , span [ A.class "oilbar__value" ] [ text valueText ]
        ]



-- HTTP


getHops : Cmd Msg
getHops =
    Http.get
        { url = "/api/hops"
        , expect = Http.expectJson GotHops (D.list decoder)
        }


httpErrorToString : Http.Error -> String
httpErrorToString e =
    case e of
        Http.BadUrl _ ->
            "URL invalide"

        Http.Timeout ->
            "Timeout"

        Http.NetworkError ->
            "Erreur réseau"

        Http.BadStatus c ->
            "Erreur " ++ String.fromInt c

        Http.BadBody _ ->
            "Réponse invalide"
