package com.ss.editor;

import com.ss.rlib.util.array.Array;
import com.ss.rlib.util.array.ArrayFactory;
import org.jetbrains.annotations.NotNull;

/**
 * The list of all file extensions.
 *
 * @author JavaSaBr
 */
public interface FileExtensions {

    /**
     * The constant JME_MATERIAL.
     */
    String JME_MATERIAL = "j3m";
    /**
     * The constant JME_MATERIAL_DEFINITION.
     */
    String JME_MATERIAL_DEFINITION = "j3md";
    /**
     * The constant JME_OBJECT.
     */
    String JME_OBJECT = "j3o";
    /**
     * The constant JME_SCENE.
     */
    String JME_SCENE = "j3s";

    /**
     * The constant JAVA_LIBRARY.
     */
    String JAVA_LIBRARY = "jar";
    /**
     * The constant JAVA_CLASS.
     */
    String JAVA_CLASS = "class";


    /**
     * The constant IMAGE_PNG.
     */
    String IMAGE_PNG = "png";
    /**
     * The constant IMAGE_JPG.
     */
    String IMAGE_JPG = "jpg";
    /**
     * The constant IMAGE_JPEG.
     */
    String IMAGE_JPEG = "jpeg";
    /**
     * The constant IMAGE_GIF.
     */
    String IMAGE_GIF = "gif";
    /**
     * The constant IMAGE_TGA.
     */
    String IMAGE_TGA = "tga";
    /**
     * The constant IMAGE_BMP.
     */
    String IMAGE_BMP = "bmp";
    /**
     * The constant IMAGE_TIFF.
     */
    String IMAGE_TIFF = "tiff";
    /**
     * The constant IMAGE_DDS.
     */
    String IMAGE_DDS = "dds";
    /**
     * The constant IMAGE_HDR.
     */
    String IMAGE_HDR = "hdr";

    /**
     * The constant AUDIO_MP3.
     */
    String AUDIO_MP3 = "mp3";
    /**
     * The constant AUDIO_OGG.
     */
    String AUDIO_OGG = "ogg";
    /**
     * The constant AUDIO_WAV.
     */
    String AUDIO_WAV = "wav";

    /**
     * The constant GLSL_VERTEX.
     */
    String GLSL_VERTEX = "vert";
    /**
     * The constant GLSL_FRAGMENT.
     */
    String GLSL_FRAGMENT = "frag";
    /**
     * The constant GLSL_TESSELLATION_CONTROL.
     */
    String GLSL_TESSELLATION_CONTROL = "tsctrl";
    /**
     * The constant GLSL_TESSELLATION_EVALUATION.
     */
    String GLSL_TESSELLATION_EVALUATION = "tseval";
    /**
     * The constant GLSL_GEOM.
     */
    String GLSL_GEOM = "geom";
    /**
     * The constant GLSL_LIB.
     */
    String GLSL_LIB = "glsllib";

    /**
     * The constant MODEL_BLENDER.
     */
    String MODEL_BLENDER = "blend";
    /**
     * The constant MODEL_FBX.
     */
    String MODEL_FBX = "fbx";
    /**
     * The constant MODEL_GLTF.
     */
    String MODEL_GLTF = "gltf";
    /**
     * The constant MODEL_OBJ.
     */
    String MODEL_OBJ = "obj";
    /**
     * The constant MODEL_SCENE.
     */
    String MODEL_SCENE = "scene";
    /**
     * The constant MODEL_MESH_XML.
     */
    String MODEL_MESH_XML = "mesh.xml";
    /**
     * The constant MODEL_XBUF.
     */
    String MODEL_XBUF = "xbuf";

    @NotNull
    Array<String> IMAGE_EXTENSIONS = ArrayFactory.asArray(
            IMAGE_PNG,
            IMAGE_JPG,
            IMAGE_JPEG,
            IMAGE_TGA,
            IMAGE_DDS,
            IMAGE_HDR,
            IMAGE_BMP,
            IMAGE_GIF,
            IMAGE_TIFF);

    @NotNull
    Array<String> TEXTURE_EXTENSIONS = ArrayFactory.asArray(
            IMAGE_PNG,
            IMAGE_JPG,
            IMAGE_JPEG,
            IMAGE_TGA,
            IMAGE_DDS,
            IMAGE_HDR);

    @NotNull Array<String> SHADER_EXTENSIONS = ArrayFactory.asArray(GLSL_FRAGMENT,
            GLSL_VERTEX,
            GLSL_TESSELLATION_CONTROL,
            GLSL_TESSELLATION_EVALUATION,
            GLSL_GEOM,
            GLSL_LIB);

    @NotNull
    Array<String> AUDIO_EXTENSIONS = ArrayFactory.asArray(
            AUDIO_MP3,
            AUDIO_WAV,
            AUDIO_OGG);
}