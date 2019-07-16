package com.linghang.repository;

import com.linghang.SellApplication;
import com.linghang.dataobject.ProductCategory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProductCategoryRepositoryTest {

    @Autowired
    private ProductCategoryRepository repository;

    @Test
    public void findById() {
        ProductCategory productCategory = repository.findById(1).get();
        Assert.assertNotNull(productCategory);
    }

    @Test
    public void save() {
        ProductCategory productCategory = new ProductCategory("男生最爱", 1);
        productCategory.setCategoryId(1);
        ProductCategory result = repository.save(productCategory);
        Assert.assertNotNull(result);
    }

    @Test
    public void findByCategoryTypeIn() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        List<ProductCategory> result = repository.findByCategoryTypeIn(list);
        Assert.assertNotEquals(0, result.size());
    }
}