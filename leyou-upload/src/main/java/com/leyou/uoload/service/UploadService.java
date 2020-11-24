package com.leyou.uoload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.beans.Transient;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class UploadService {

    private static final List<String> CONTENT_TYPES= Arrays.asList("image/gif","image/jpeg");

    @Autowired
    private FastFileStorageClient storageClient;
    //log日志
    private static final Logger LOGGER= LoggerFactory.getLogger(UploadService.class);
    @Transient
    public String uploadImage(MultipartFile file) throws IOException {

        String originalFilename = file.getOriginalFilename();
        //校验文件类型
        String contentType = file.getContentType();
        if(!CONTENT_TYPES.contains(contentType)){
            //输出log日志
            LOGGER.info("文件类型不合法：{}",originalFilename);
            return null;
        }
        //校验文件内容
        BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
        if(bufferedImage==null){
            LOGGER.info("文件内容不合法: {}",originalFilename);
            return null;
        }
        //保存到服务器
        //file.transferTo(new File("D:\\A_Project\\leyous\\images\\"+originalFilename));
        //获取图片后缀名
        String ext = StringUtils.substringAfterLast(originalFilename, ".");
        StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), ext, null);

        //返回url，进行回写
        return "http://39.106.140.83/"+storePath.getFullPath();
    }
}
