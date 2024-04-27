package com.greybox.projectmesh.networking

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream

class HttpServer() : NanoHTTPD(8080) {

    // Contains a map of:
    // (URL, ACTION) --> Method
    private var endpoints = emptyMap<Pair<String,String>,Function<Response>>()
    override fun serve(session: IHTTPSession?): Response {
        //return super.serve(session)
        Log.d("DEBUG", "Got a request: ${session?.remoteIpAddress}")
        return newFixedLengthResponse(Response.Status.NOT_FOUND,"text","hello".byteInputStream(),5)
    }

//
//    @Override
//    public Response handle(IHTTPSession session) {
//        String uri = session.getUri();
//        if(uri.equalsIgnoreCase("/hello.html")){
//            String msg = "<html><body><h1>Hello World!</h1></body></html>";
//            return  return Response.newFixedLengthResponse(Status.OK, "text/html", msg);
//        }
//        return Response.newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Not found");
//    }

}