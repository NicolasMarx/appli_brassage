module Main exposing (main)

import Browser
import Browser.Navigation as Nav
import Html exposing (Html, a, div, span, text)
import Html.Attributes as A
import Url exposing (Url)
import Tuple

import Route
import Pages.Login as Login
import Pages.Home as Home
import Pages.Hops as Hops
import Pages.Upload as Upload
import Pages.Referentials as Refs


-- Flags (passés par boot.js)
type alias Flags =
    { csrf : Maybe String
    , auth : Bool              -- ✨ nouveau : session déjà présente ?
    }


-- MODEL

type alias Model =
    { key : Nav.Key
    , route : Route.Route
    , login : Login.Model
    , hops : Maybe Hops.Model
    , upload : Maybe Upload.Model
    , csrf : Maybe String
    , auth : Bool              -- ✨
    }


-- MSG

type Msg
    = UrlChanged Url
    | LinkClicked Browser.UrlRequest
    | LoginMsg Login.Msg
    | HopsMsg Hops.Msg
    | UploadMsg Upload.Msg


-- MAIN

main : Program Flags Model Msg
main =
    Browser.application
        { init = \flags url key -> init flags url key
        , view = view
        , update = update
        , subscriptions = \_ -> Sub.none
        , onUrlChange = UrlChanged
        , onUrlRequest = LinkClicked
        }


-- INIT

init : Flags -> Url -> Nav.Key -> ( Model, Cmd Msg )
init flags url key =
    let
        rawRoute =
            Maybe.withDefault Route.RLogin (Route.fromUrl url)

        route =
            if flags.auth then
                rawRoute
            else
                Route.RLogin
    in
    ( { key = key
      , route = route
      , login = Login.init flags.csrf           -- ✅ on passe le CSRF au sous-modèle Login
      , hops = Nothing
      , upload = Nothing
      , csrf = flags.csrf
      , auth = flags.auth                        -- ✨
      }
    , if (not flags.auth) && (rawRoute /= Route.RLogin) then
        -- réaligne l'URL du navigateur si on arrive directement sur /admin/xxx sans session
        Nav.replaceUrl key (Route.href Route.RLogin)
      else
        Cmd.none
    )


-- UPDATE

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        LinkClicked req ->
            case req of
                Browser.Internal url ->
                    let
                        next =
                            Maybe.withDefault Route.RLogin (Route.fromUrl url)
                    in
                    if (not model.auth) && (next /= Route.RLogin) then
                        -- bloque la nav client si pas logué
                        ( model, Nav.replaceUrl model.key (Route.href Route.RLogin) )
                    else
                        ( model, Nav.pushUrl model.key (Url.toString url) )

                Browser.External href ->
                    ( model, Nav.load href )

        UrlChanged url ->
            let
                r =
                    Maybe.withDefault Route.RLogin (Route.fromUrl url)
            in
            if (not model.auth) && (r /= Route.RLogin) then
                ( { model | route = Route.RLogin }, Cmd.none )
            else
                ( { model | route = r }, Cmd.none )

        LoginMsg sub ->
            let
                ( m, cmd ) =
                    -- 💡 Login.update devrait faire Nav.load "/admin" après succès.
                    -- On ne change pas sa signature ici, on se contente de relayer ses Cmd.
                    Login.update model.key sub model.login
            in
            ( { model | login = m }, Cmd.map LoginMsg cmd )

        HopsMsg sub ->
            case model.hops of
                Nothing ->
                    ( model, Cmd.none )

                Just hm ->
                    let
                        ( hm2, cmd ) =
                            Hops.update sub hm
                    in
                    ( { model | hops = Just hm2 }, Cmd.map HopsMsg cmd )

        UploadMsg sub ->
            case model.upload of
                Nothing ->
                    ( model, Cmd.none )

                Just um ->
                    let
                        ( um2, cmd ) =
                            Upload.update sub um
                    in
                    ( { model | upload = Just um2 }, Cmd.map UploadMsg cmd )


-- VIEW

view : Model -> Browser.Document Msg
view model =
    { title = "Admin"
    , body =
        [ navBar model.auth
        , case model.route of
            Route.RLogin ->
                Html.map LoginMsg (Login.view model.login)

            Route.RHome ->
                Home.view

            Route.RHops ->
                case model.hops of
                    Nothing ->
                        text ""

                    Just hm ->
                        Html.map HopsMsg (Hops.view hm)

            Route.RUpload ->
                case model.upload of
                    Nothing ->
                        text ""

                    Just um ->
                        Html.map UploadMsg (Upload.view um)

            Route.RReferentials ->
                Refs.view
        ]
    }


navBar : Bool -> Html Msg
navBar isAuth =
    if not isAuth then
        -- navbar masquée tant que non logué
        div [] []
    else
        div [ A.class "navbar" ]
            [ a [ A.href (Route.href Route.RHome) ] [ text "Accueil admin" ]
            , span [ A.class "spacer" ] []
            , a [ A.href (Route.href Route.RHops) ] [ text "Houblons" ]
            , a [ A.href (Route.href Route.RUpload) ] [ text "Upload" ]
            , a [ A.href (Route.href Route.RReferentials) ] [ text "Référentiels" ]
            ]
